#!/usr/bin/env bash
set -uo pipefail

CONTEXT="kind-banksolution"
KAFKA_NAMESPACE="platform"
KAFKA_POD="bank-kafka-dual-role-0"
KAFKA_CONTAINER="kafka"
BOOTSTRAP="bank-kafka-kafka-bootstrap:9092"
KAFKA_BIN="/opt/kafka/bin"
PROBE_TIMEOUT_SECONDS=25

CHECKS=(
    "allow banking/risk-engine-service       describe       risk.assessment.requested"
    "deny  banking/risk-engine-service       describe       ledger.posting.requested"
    "deny  banking/risk-engine-service       produce        ledger.posting.requested"
    "deny  banking/risk-engine-service       consume-group  risk.assessment.requested:ledger-group"
    "allow banking/ledger-service            describe       ledger.posting.requested"
    "deny  banking/payment-service           produce        ledger.posting.requested"
    "deny  banking/payment-service           produce        payment-completed-events"
    "deny  ai/marl-orchestrator              produce        payment-completed-events"
    "deny  ai/marl-orchestrator              describe       ledger.posting.requested"
    "deny  anonymous                         describe       risk.assessment.requested"
    "deny  wrong-password                    describe       risk.assessment.requested"
)

kafka_exec() {
    kubectl --context "$CONTEXT" --namespace "$KAFKA_NAMESPACE" exec -i "$KAFKA_POD" --container "$KAFKA_CONTAINER" -- "$@"
}

client_properties() {
    local principal="$1"
    local timeouts="request.timeout.ms=5000
default.api.timeout.ms=10000
max.block.ms=8000
delivery.timeout.ms=8000"
    case "$principal" in
        anonymous)
            printf "security.protocol=PLAINTEXT\n%s\n" "$timeouts"
            ;;
        wrong-password)
            printf 'security.protocol=SASL_PLAINTEXT\nsasl.mechanism=SCRAM-SHA-512\nsasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required username="risk-engine-service" password="not-the-password";\n%s\n' "$timeouts"
            ;;
        *)
            local namespace="${principal%%/*}" service="${principal#*/}" jaas_config
            jaas_config=$(kubectl --context "$CONTEXT" --namespace "$namespace" get secret "$service-kafka-credentials" \
                --output jsonpath='{.data.sasl\.jaas\.config}' | base64 --decode)
            printf "security.protocol=SASL_PLAINTEXT\nsasl.mechanism=SCRAM-SHA-512\nsasl.jaas.config=%s\n%s\n" "$jaas_config" "$timeouts"
            ;;
    esac
}

probe() {
    local properties_file="$1" action="$2" target="$3"
    case "$action" in
        describe)
            kafka_exec timeout "$PROBE_TIMEOUT_SECONDS" "$KAFKA_BIN/kafka-topics.sh" --bootstrap-server "$BOOTSTRAP" \
                --command-config "$properties_file" --describe --topic "$target" 2>&1
            ;;
        produce)
            echo "acl-probe" | kafka_exec timeout "$PROBE_TIMEOUT_SECONDS" "$KAFKA_BIN/kafka-console-producer.sh" \
                --bootstrap-server "$BOOTSTRAP" --producer.config "$properties_file" --topic "$target" 2>&1
            ;;
        consume-group)
            kafka_exec timeout "$PROBE_TIMEOUT_SECONDS" "$KAFKA_BIN/kafka-console-consumer.sh" --bootstrap-server "$BOOTSTRAP" \
                --consumer.config "$properties_file" --topic "${target%%:*}" --group "${target##*:}" \
                --max-messages 1 --timeout-ms 8000 2>&1
            ;;
    esac
}

observe() {
    local action="$1" target="$2" output="$3"
    if grep -qiE "not authorized|authorizationexception|authentication failed|saslauthentication|timed out|timeoutexception" <<<"$output"; then
        echo "deny"
    elif [[ "$action" == "describe" ]] && ! grep -q "Topic: $target" <<<"$output"; then
        echo "deny"
    else
        echo "allow"
    fi
}

failures=0
for check in "${CHECKS[@]}"; do
    read -r expectation principal action target <<<"$check"
    properties_file="/tmp/acl-probe-${principal//\//-}.properties"
    client_properties "$principal" | kafka_exec sh -c "cat > $properties_file"
    output=$(probe "$properties_file" "$action" "$target")
    observed=$(observe "$action" "$target" "$output")

    if [[ "$observed" == "$expectation" ]]; then
        result="PASS"
    else
        result="FAIL"
        failures=$((failures + 1))
    fi
    printf "%-4s  expected %-5s  observed %-5s  %-34s %-14s %s\n" "$result" "$expectation" "$observed" "$principal" "$action" "$target"
done

kafka_exec sh -c "rm -f /tmp/acl-probe-*.properties"
echo "${#CHECKS[@]} checks, $failures failed"
[[ $failures -eq 0 ]]

#!/usr/bin/env bash
set -uo pipefail

CONTEXT="kind-banksolution"
CONNECT_TIMEOUT_SECONDS=3
PROBE_POD="network-policy-probe"
PROBE_IMAGE="ghcr.io/cloudnative-pg/postgresql:16"

CHECKS=(
    "allow ai/marl-orchestrator              ai-postgres-rw.ai:5432"
    "deny  ai/marl-orchestrator              bank-postgres-rw.banking:5432"
    "allow ai/marl-orchestrator              configuration-service.banking:5009"
    "deny  ai/transaction-pattern-agent      bank-kafka-kafka-bootstrap.platform:9092"
    "deny  ai/transaction-pattern-agent      bank-postgres-rw.banking:5432"
    "allow banking/payment-service           account-service.banking:5002"
    "allow banking/payment-service           api.exchangerate-api.com:443"
    "deny  banking/payment-service           neo4j.banking:7687"
    "deny  banking/customer-service          bank-kafka-kafka-bootstrap.platform:9092"
    "deny  banking/customer-service          api.exchangerate-api.com:443"
    "allow banking/network-topology-service  neo4j.banking:7687"
    "allow banking/ledger-service            tigerbeetle.banking:3000"
    "deny  banking/risk-engine-service        tigerbeetle.banking:3000"
    "allow banking/backoffice-gateway         payment-engine-service.banking:5004"
    "deny  probe/banking                      payment-engine-service.banking:5004"
    "allow banking/payment-service            account-service.banking:5002"
    "deny  probe/banking                      account-service.banking:5002"
    "allow ai/marl-orchestrator               transaction-pattern-agent.ai:1001"
    "deny  probe/ai                           transaction-pattern-agent.ai:1001"
    "allow banking/risk-engine-service        bank-kafka-kafka-bootstrap.platform:9092"
    "deny  probe/banking                      bank-kafka-kafka-bootstrap.platform:9092"
)

can_connect() {
    local namespace="$1" deployment="$2" target_host="$3" target_port="$4"
    if [[ "$deployment" == "probe" ]]; then
        kubectl --context "$CONTEXT" --namespace "$namespace" exec "$PROBE_POD" -- \
            bash -c "timeout $CONNECT_TIMEOUT_SECONDS bash -c '</dev/tcp/$target_host/$target_port'" >/dev/null 2>&1
    else
        kubectl --context "$CONTEXT" --namespace "$namespace" exec "deploy/$deployment" --container "$deployment" -- \
            bash -c "timeout $CONNECT_TIMEOUT_SECONDS bash -c '</dev/tcp/$target_host/$target_port'" >/dev/null 2>&1
    fi
}

start_probe_pods() {
    for namespace in banking ai; do
        kubectl --context "$CONTEXT" --namespace "$namespace" run "$PROBE_POD" --image "$PROBE_IMAGE" \
            --image-pull-policy IfNotPresent --restart Never --command -- sleep 600 >/dev/null
    done
    for namespace in banking ai; do
        kubectl --context "$CONTEXT" --namespace "$namespace" wait --for condition=Ready "pod/$PROBE_POD" --timeout 120s >/dev/null
    done
}

stop_probe_pods() {
    for namespace in banking ai; do
        kubectl --context "$CONTEXT" --namespace "$namespace" delete pod "$PROBE_POD" --wait=false >/dev/null 2>&1
    done
}

trap stop_probe_pods EXIT
start_probe_pods

failures=0
for check in "${CHECKS[@]}"; do
    read -r expectation source target <<<"$check"
    if [[ "$source" == probe/* ]]; then
        namespace="${source#probe/}"
        deployment="probe"
    else
        namespace="${source%%/*}"
        deployment="${source#*/}"
    fi
    target_host="${target%:*}"
    target_port="${target##*:}"

    if can_connect "$namespace" "$deployment" "$target_host" "$target_port"; then
        observed="allow"
    else
        observed="deny"
    fi

    if [[ "$observed" == "$expectation" ]]; then
        result="PASS"
    else
        result="FAIL"
        failures=$((failures + 1))
    fi
    printf "%-4s  expected %-5s  observed %-5s  %-38s -> %s\n" "$result" "$expectation" "$observed" "$source" "$target"
done

echo "${#CHECKS[@]} checks, $failures failed"
[[ $failures -eq 0 ]]

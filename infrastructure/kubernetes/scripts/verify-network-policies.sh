#!/usr/bin/env bash
set -uo pipefail

CONTEXT="kind-banksolution"
CONNECT_TIMEOUT_SECONDS=3

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
)

can_connect() {
    local namespace="$1" deployment="$2" target_host="$3" target_port="$4"
    kubectl --context "$CONTEXT" --namespace "$namespace" exec "deploy/$deployment" --container "$deployment" -- \
        bash -c "timeout $CONNECT_TIMEOUT_SECONDS bash -c '</dev/tcp/$target_host/$target_port'" >/dev/null 2>&1
}

failures=0
for check in "${CHECKS[@]}"; do
    read -r expectation source target <<<"$check"
    namespace="${source%%/*}"
    deployment="${source#*/}"
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

package org.banksolution.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.domain.AccountMovement;
import org.banksolution.domain.AccountNeighbourhood;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
@Slf4j
public class NetworkGraphRepository {

    private final Driver neo4jDriver;

    public AccountNeighbourhood getAccountNeighbourhood(String accountId) {
        try (Session session = neo4jDriver.session()) {
            Record structural = fetchStructuralCounts(session, accountId);
            if (structural == null) {
                log.warn("Account not found in graph: {}", accountId);
                return AccountNeighbourhood.empty(accountId);
            }

            Record movements = fetchMovements(session, accountId);

            return new AccountNeighbourhood(
                    accountId,
                    new HashSet<>(structural.get("senderAccountIds").asList(Value::asString)),
                    new HashSet<>(structural.get("receiverAccountIds").asList(Value::asString)),
                    structural.get("cycle3Count").asInt(),
                    structural.get("twoHopOutReach").asInt(),
                    toAccountMovements(movements, "incomingMovements"),
                    toAccountMovements(movements, "outgoingMovements"));
        }
    }

    private Record fetchStructuralCounts(Session session, String accountId) {
        String query = """
                MATCH (a:Account {accountId: $accountId})
                OPTIONAL MATCH (sender:Account)-[:TRANSFERRED_TO]->(a)
                WITH a, COLLECT(DISTINCT sender.accountId) AS senderAccountIds
                OPTIONAL MATCH (a)-[:TRANSFERRED_TO]->(receiver:Account)
                WITH a, senderAccountIds, COLLECT(DISTINCT receiver.accountId) AS receiverAccountIds
                OPTIONAL MATCH (a)-[:TRANSFERRED_TO]->(v:Account)-[:TRANSFERRED_TO]->(w:Account)
                WHERE v <> a AND w <> a AND w <> v
                WITH a, senderAccountIds, receiverAccountIds, COUNT(DISTINCT w) AS twoHopOutReach
                OPTIONAL MATCH (a)-[:TRANSFERRED_TO]->(x:Account)-[:TRANSFERRED_TO]->(y:Account)-[:TRANSFERRED_TO]->(a)
                WHERE x <> a AND y <> a AND x <> y
                RETURN senderAccountIds,
                       receiverAccountIds,
                       twoHopOutReach,
                       COUNT(DISTINCT y) AS cycle3Count
                """;

        Result result = session.run(query, Values.parameters("accountId", accountId));
        return result.hasNext() ? result.next() : null;
    }

    private Record fetchMovements(Session session, String accountId) {
        String query = """
                MATCH (a:Account {accountId: $accountId})
                OPTIONAL MATCH (sender:Account)-[incoming:TRANSFERRED_TO]->(a)
                WITH a, COLLECT({
                    counterparty: sender.accountId,
                    amount: incoming.amount,
                    timestamp: incoming.timestamp
                }) AS incomingMovements
                OPTIONAL MATCH (a)-[outgoing:TRANSFERRED_TO]->(receiver:Account)
                RETURN incomingMovements,
                       COLLECT({
                           counterparty: receiver.accountId,
                           amount: outgoing.amount,
                           timestamp: outgoing.timestamp
                       }) AS outgoingMovements
                """;

        return session.run(query, Values.parameters("accountId", accountId)).next();
    }

    private List<AccountMovement> toAccountMovements(Record movements, String field) {
        return movements.get(field).asList(value -> value.get("counterparty").isNull()
                        ? null
                        : new AccountMovement(
                        value.get("counterparty").asString(),
                        value.get("amount").asDouble(),
                        value.get("timestamp").asLong()))
                .stream()
                .filter(Objects::nonNull)
                .toList();
    }

    public void createTransactionRelationship(
            String sourceAccountId,
            String destAccountId,
            String paymentId,
            double amount,
            String fromCurrency,
            String toCurrency,
            String paymentType,
            long timestamp,
            boolean riskCheckPassed) {
        try (Session session = neo4jDriver.session()) {
            String query = """
                    MERGE (source:Account {accountId: $sourceAccountId})
                    ON CREATE SET source.createdAt      = datetime(),
                                  source.lastActivityAt = datetime(),
                                  source.transactionCount = 1
                    ON MATCH  SET source.lastActivityAt  = datetime(),
                                  source.transactionCount = source.transactionCount + 1
                    MERGE (dest:Account {accountId: $destAccountId})
                    ON CREATE SET dest.createdAt      = datetime(),
                                  dest.lastActivityAt = datetime(),
                                  dest.transactionCount = 1
                    ON MATCH  SET dest.lastActivityAt  = datetime(),
                                  dest.transactionCount = dest.transactionCount + 1
                    MERGE (source)-[r:TRANSFERRED_TO {paymentId: $paymentId}]->(dest)
                    ON CREATE SET r.amount        = $amount,
                                  r.fromCurrency  = $fromCurrency,
                                  r.toCurrency    = $toCurrency,
                                  r.paymentType   = $paymentType,
                                  r.timestamp     = $timestamp,
                                  r.riskCheckPassed = $riskCheckPassed
                    """;

            session.run(query, Values.parameters(
                    "sourceAccountId", sourceAccountId,
                    "destAccountId", destAccountId,
                    "paymentId", paymentId,
                    "amount", amount,
                    "fromCurrency", fromCurrency,
                    "toCurrency", toCurrency,
                    "paymentType", paymentType,
                    "timestamp", timestamp,
                    "riskCheckPassed", riskCheckPassed
            )).consume();

            log.debug("Transaction relationship merged: {} -> {} for payment: {} riskCheckPassed: {}",
                    sourceAccountId,
                    destAccountId,
                    paymentId,
                    riskCheckPassed);
        }
    }
}

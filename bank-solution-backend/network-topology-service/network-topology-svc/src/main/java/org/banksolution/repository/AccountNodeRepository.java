package org.banksolution.repository;

import org.banksolution.node.AccountNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountNodeRepository extends Neo4jRepository<AccountNode, String> {

    @Query("""
        MERGE (a:Account {accountId: $accountId})
        ON CREATE SET a.customerId = COALESCE($customerId, 'UNKNOWN'),
                      a.createdAt = datetime(),
                      a.lastActivityAt = datetime(),
                      a.transactionCount = 1
        ON MATCH SET a.lastActivityAt = datetime(),
                     a.transactionCount = a.transactionCount + 1,
                     a.customerId = CASE
                         WHEN a.customerId = 'UNKNOWN' AND $customerId IS NOT NULL 
                         THEN $customerId 
                         ELSE a.customerId 
                     END
        RETURN a
        """)
    void mergeAccount(@Param("accountId") String accountId,
                      @Param("customerId") String customerId);
}

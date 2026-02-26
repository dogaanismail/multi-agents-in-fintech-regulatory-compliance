package org.banksolution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GraphAlgorithmScheduledService {

    private final Driver neo4jDriver;
    private static final String GRAPH_NAME_KEY = "graphName";
    private static final String GRAPH_NAME_DIRECTED = "transactionGraphDirected";
    private static final String GRAPH_NAME_UNDIRECTED = "transactionGraphUndirected";

    public void computeAllMetrics() {
        log.info("Starting scheduled graph algorithm computation");

        try {
            if (!hasEnoughNodes()) {
                log.info("Not enough nodes for graph algorithms, skipping");
                return;
            }

            refreshGraphProjections();
            computePageRank();
            computeBetweennessCentrality();
            computeClosenessCentrality();
            computeEigenvectorCentrality();
            computeLocalClusteringCoefficient();
            computeCommunities();
            dropGraphProjections();

            log.info("Graph algorithm computation completed successfully");
        } catch (Exception e) {
            log.error("Failed to compute graph algorithms", e);
        }
    }

    private boolean hasEnoughNodes() {
        try (Session session = neo4jDriver.session()) {
            Result result = session.run("MATCH (a:Account) RETURN COUNT(a) AS count");
            if (result.hasNext()) {
                long count = result.next().get("count").asLong();
                return count >= 2;
            }
            return false;
        }
    }

    private void refreshGraphProjections() {
        try (Session session = neo4jDriver.session()) {
            session.run("CALL gds.graph.drop($graphName, false)",
                    Values.parameters(GRAPH_NAME_KEY, GRAPH_NAME_DIRECTED)).consume();
            session.run("CALL gds.graph.drop($graphName, false)",
                    Values.parameters(GRAPH_NAME_KEY, GRAPH_NAME_UNDIRECTED)).consume();

            session.run("""
                    CALL gds.graph.project(
                        $graphName,
                        'Account',
                        {
                            TRANSFERRED_TO: {
                                orientation: 'NATURAL',
                                properties: ['amount']
                            }
                        }
                    )
                    """, Values.parameters(GRAPH_NAME_KEY, GRAPH_NAME_DIRECTED)).consume();

            session.run("""
                    CALL gds.graph.project(
                        $graphName,
                        'Account',
                        {
                            TRANSFERRED_TO: {
                                orientation: 'UNDIRECTED',
                                properties: ['amount']
                            }
                        }
                    )
                    """, Values.parameters(GRAPH_NAME_KEY, GRAPH_NAME_UNDIRECTED)).consume();

            log.debug("Graph projections created");
        }
    }

    private void computePageRank() {
        try (Session session = neo4jDriver.session()) {
            session.run("""
                    CALL gds.pageRank.write($graphName, {
                        writeProperty: 'pagerank',
                        dampingFactor: 0.85,
                        maxIterations: 20
                    })
                    """, Values.parameters(GRAPH_NAME_KEY, GRAPH_NAME_DIRECTED)).consume();
            log.debug("PageRank computed");
        }
    }

    private void computeBetweennessCentrality() {
        try (Session session = neo4jDriver.session()) {
            session.run("""
                    CALL gds.betweenness.write($graphName, {
                        writeProperty: 'betweennessCentrality'
                    })
                    """, Values.parameters(GRAPH_NAME_KEY, GRAPH_NAME_DIRECTED)).consume();
            log.debug("Betweenness centrality computed");
        }
    }

    private void computeClosenessCentrality() {
        try (Session session = neo4jDriver.session()) {
            session.run("""
                    CALL gds.closeness.write($graphName, {
                        writeProperty: 'closenessCentrality'
                    })
                    """, Values.parameters(GRAPH_NAME_KEY, GRAPH_NAME_UNDIRECTED)).consume();
            log.debug("Closeness centrality computed");
        }
    }

    private void computeEigenvectorCentrality() {
        try (Session session = neo4jDriver.session()) {
            session.run("""
                    CALL gds.eigenvector.write($graphName, {
                        writeProperty: 'eigenvectorCentrality',
                        maxIterations: 20
                    })
                    """, Values.parameters(GRAPH_NAME_KEY, GRAPH_NAME_DIRECTED)).consume();
            log.debug("Eigenvector centrality computed");
        }
    }

    private void computeLocalClusteringCoefficient() {
        try (Session session = neo4jDriver.session()) {
            session.run("""
                    CALL gds.localClusteringCoefficient.write($graphName, {
                        writeProperty: 'clusteringCoefficient'
                    })
                    """, Values.parameters(GRAPH_NAME_KEY, GRAPH_NAME_UNDIRECTED)).consume();
            log.debug("Clustering coefficient computed");
        }
    }

    private void computeCommunities() {
        try (Session session = neo4jDriver.session()) {
            session.run("""
                    CALL gds.louvain.write($graphName, {
                        writeProperty: 'community'
                    })
                    """, Values.parameters(GRAPH_NAME_KEY, GRAPH_NAME_UNDIRECTED)).consume();
            log.debug("Community detection completed");
        }
    }

    private void dropGraphProjections() {
        try (Session session = neo4jDriver.session()) {
            session.run("CALL gds.graph.drop($graphName, false)",
                    Values.parameters(GRAPH_NAME_KEY, GRAPH_NAME_DIRECTED)).consume();
            session.run("CALL gds.graph.drop($graphName, false)",
                    Values.parameters(GRAPH_NAME_KEY, GRAPH_NAME_UNDIRECTED)).consume();
        }
    }
}
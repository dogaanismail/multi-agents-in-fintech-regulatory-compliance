package org.banksolution.common.containers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

public class TigerBeetleContainer extends GenericContainer<TigerBeetleContainer> {

    private static final DockerImageName IMAGE = DockerImageName.parse("ghcr.io/tigerbeetle/tigerbeetle:0.17.9");
    private static final int TIGERBEETLE_PORT = 3000;
    private static final String DATA_FILE = "/tmp/0_0.tigerbeetle";
    private static final String LISTENING_LOG_MESSAGE = ".*listening on.*";

    public TigerBeetleContainer() {
        super(IMAGE);

        withExposedPorts(TIGERBEETLE_PORT);
        // TigerBeetle needs io_uring, which Docker's default seccomp profile blocks.
        withCreateContainerCmdModifier(command -> {
            command.withEntrypoint("sh");
            command.getHostConfig().withSecurityOpts(List.of("seccomp=unconfined"));
        });

        withCommand("-c", formatThenStart());
        waitingFor(Wait.forLogMessage(LISTENING_LOG_MESSAGE, 1));
    }

    public String address() {
        return getHost() + ":" + getMappedPort(TIGERBEETLE_PORT);
    }

    private static String formatThenStart() {
        return String.join(" ",
                "/tigerbeetle format --cluster=0 --replica=0 --replica-count=1 --development", DATA_FILE,
                "&& /tigerbeetle start --addresses=0.0.0.0:" + TIGERBEETLE_PORT + " --development", DATA_FILE);
    }
}

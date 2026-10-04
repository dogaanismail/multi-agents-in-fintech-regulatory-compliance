package org.banksolution.scheduling.config;

import com.github.kagkarlsson.scheduler.SchedulerName;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class SchedulerInstanceNames {

    static final String UNKNOWN_HOSTNAME = "unknown-host";

    private SchedulerInstanceNames() {
    }

    public static SchedulerName resolveSchedulerInstanceName(String applicationName) {
        return new SchedulerName.Fixed(deriveSchedulerInstanceName(applicationName, resolveLocalHostname()));
    }

    static String deriveSchedulerInstanceName(
            String applicationName,
            String hostname) {

        if (applicationName == null || applicationName.isBlank()) {
            return hostname;
        }

        return applicationName + "@" + hostname;
    }

    private static String resolveLocalHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException _) {
            return UNKNOWN_HOSTNAME;
        }
    }
}

package org.banksolution.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The TigerBeetle client rejects hostnames outright with "Replica addresses format is
 * invalid", so Docker service names must be resolved to literal IPs before it is built.
 */
class TigerBeetleAddressesTest {

    @Test
    void shouldResolveAHostnameToALiteralIpAddress() {
        assertThat(TigerBeetleAddresses.resolve(List.of("localhost:3033")))
                .containsExactly("127.0.0.1:3033");
    }

    @Test
    void shouldLeaveAnIpAddressUnchanged() {
        assertThat(TigerBeetleAddresses.resolve(List.of("127.0.0.1:3033")))
                .containsExactly("127.0.0.1:3033");
    }

    @Test
    void shouldLeaveAPortOnlyAddressUnchanged() {
        assertThat(TigerBeetleAddresses.resolve(List.of("3033"))).containsExactly("3033");
    }

    @Test
    void shouldResolveEveryReplicaAddress() {
        assertThat(TigerBeetleAddresses.resolve(List.of("localhost:3001", "127.0.0.1:3002")))
                .containsExactly("127.0.0.1:3001", "127.0.0.1:3002");
    }

    @Test
    void shouldPreserveAddressOrderBecauseReplicaIndexDependsOnIt() {
        assertThat(TigerBeetleAddresses.resolve(List.of("127.0.0.3:3003", "127.0.0.1:3001", "127.0.0.2:3002")))
                .containsExactly("127.0.0.3:3003", "127.0.0.1:3001", "127.0.0.2:3002");
    }

    @Test
    void shouldLeaveABracketedIpv6AddressUnchanged() {
        assertThat(TigerBeetleAddresses.resolve(List.of("[::1]:3033"))).containsExactly("[::1]:3033");
    }

    @Test
    void shouldReturnNoAddressesForAnEmptyList() {
        assertThat(TigerBeetleAddresses.resolve(List.of())).isEmpty();
    }

    @Test
    void shouldFailFastWhenTheHostCannotBeResolved() {
        List<String> unresolvableAddresses = List.of("tigerbeetle-does-not-exist.invalid:3000");

        assertThatThrownBy(() -> TigerBeetleAddresses.resolve(unresolvableAddresses))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot resolve TigerBeetle host");
    }
}

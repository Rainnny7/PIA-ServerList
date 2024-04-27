package me.braydon.pia;

import lombok.*;

/**
 * A representation of a PIA server.
 *
 * @author Braydon
 */
@AllArgsConstructor @Getter @EqualsAndHashCode(onlyExplicitlyIncluded = true) @ToString
public final class PIAServer {
    /**
     * The IPv4 address of this server.
     */
    @EqualsAndHashCode.Include @NonNull private final String ip;

    /**
     * The region of this server.
     */
    @NonNull private final String region;

    /**
     * The unix time of when this server was last seen.
     */
    private final long lastSeen;
}
package org.kylan1940.serverassistantai.ai;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final Map<UUID, Long> cooldowns = new HashMap<>();

    /**
     * Checks whether a player is currently on cooldown.
     *
     * @param playerId Player UUID
     * @param cooldownSeconds Cooldown duration in seconds
     * @return Remaining cooldown in seconds, or 0 if the player can make a request
     */
    public long getRemaining(UUID playerId, long cooldownSeconds) {

        Long lastRequest = cooldowns.get(playerId);

        if (lastRequest == null) {
            return 0;
        }

        long cooldownMillis = cooldownSeconds * 1000L;
        long elapsed = System.currentTimeMillis() - lastRequest;
        long remaining = cooldownMillis - elapsed;

        if (remaining <= 0) {
            cooldowns.remove(playerId);
            return 0;
        }

        return (remaining + 999) / 1000;
    }

    /**
     * Starts or resets the cooldown for a player.
     *
     * @param playerId Player UUID
     */
    public void start(UUID playerId) {
        cooldowns.put(playerId, System.currentTimeMillis());
    }

    /**
     * Removes a player's cooldown.
     *
     * @param playerId Player UUID
     */
    public void remove(UUID playerId) {
        cooldowns.remove(playerId);
    }

    /**
     * Clears all cooldowns.
     */
    public void clear() {
        cooldowns.clear();
    }
}
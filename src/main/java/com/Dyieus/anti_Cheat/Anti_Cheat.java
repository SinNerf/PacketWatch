package com.Dyieus.anti_Cheat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Anti_Cheat extends JavaPlugin {
    // Save individual players last location
    private final Map<UUID, Location> lastPlaces = new HashMap<>();
    private final Map<UUID, Integer> fastSeconds = new HashMap<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        new BukkitRunnable() {
            @Override
            public void run() {
                checkEveryone();
            }
        }.runTaskTimer(this, 20L, 20L);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void checkEveryone() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkSpeed(player);
        }
    }

    private void checkSpeed(Player player) {
        UUID id = player.getUniqueId();
        String name = player.getName();

        if (player.isFlying() || player.isGliding() || player.isInsideVehicle()) {
            lastPlaces.put(id, player.getLocation().clone());
            fastSeconds.put(id, 0);
            return;
        }

        Location now = player.getLocation();
        Location before = lastPlaces.get(player.getUniqueId());
        lastPlaces.put(player.getUniqueId(), now.clone());

        if (before == null || before.getWorld() == null || now.getWorld() == null || !before.getWorld().equals(now.getWorld()) ) {
            fastSeconds.put(id, 0);
            return;
        }

        double dx = now.getX() - before.getX();
        double dz = now.getZ() - before.getZ();
        double blocks = Math.sqrt(dx * dx + dz * dz);

        if (blocks > 10) {
            int strike = fastSeconds.getOrDefault(id, 0) + 1;
            fastSeconds.put(id, strike);

            if (strike >= 3) {
                getLogger().info(name + " moved " + blocks + " in 1 second. || " + name + " was flagged " + strike + " times!");
            }
        } else {
            fastSeconds.put(id, 0);
        }
    }

}

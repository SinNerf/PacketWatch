package com.Dyieus.anti_Cheat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Anti_Cheat extends JavaPlugin implements Listener {

    // Save individual players data ( variable names describe what they save clearly )
    private final Map<UUID, Location> lastPlaces = new HashMap<>();
    private final Map<UUID, Integer> fastSeconds = new HashMap<>();
    private final Map<UUID, Integer> airSecond = new HashMap<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(this, this);

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

    // getting every player that is online and giving it to other method that is responsible for tracking users data.
    private void checkEveryone() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            watch(player);
        }
    }

    // responsible for keeping watch over players' data. < thank you autocorrect <3 >
    private void watch(Player player) {
        UUID id = player.getUniqueId();
        Location now = player.getLocation();
        Location before = lastPlaces.get(id);
        lastPlaces.put(id, now.clone());

        if (player.isFlying() || player.isGliding() || player.isInsideVehicle()) {
            airSecond.put(id, 0);
            fastSeconds.put(id, 0);
            return;
        }

        if (before == null || before.getWorld() == null || now.getWorld() == null || !before.getWorld().equals(now.getWorld()) ) {
            airSecond.put(id, 0);
            fastSeconds.put(id, 0);
            return;
        }

        checkSpeed(player, before, now);
        checkFly(player, before, now);
    }

    // responsible for speed hacks.
    private void checkSpeed(Player player, Location before, Location now) {
        UUID id = player.getUniqueId();
        String name = player.getName();

        double dx = now.getX() - before.getX();
        double dz = now.getZ() - before.getZ();
        double blocks = Math.sqrt(dx * dx + dz * dz);

        int limit = 10;
        if (speedBlock(now) || speedBlock(before)) {
            limit = 25;
        }
        if (blocks > limit) {
            int strike = fastSeconds.getOrDefault(id, 0) + 1;
            fastSeconds.put(id, strike);

            if (strike >= 3) {
                getLogger().info(name + " moved " + blocks + " in 1 second. || " + name + " was flagged " + strike + " times!");
                player.teleport(before);
                player.setVelocity(new Vector(0, 0, 0));
                lastPlaces.put(id, before.clone());
            }
        } else {
            fastSeconds.put(id, 0);
        }
    }

    // Checks for every block under player that can speed up player beyond base limit.
    private boolean speedBlock(Location spot) {
        Location under = spot.clone();
        for (int step = 0; step < 4; step++) {
            under.subtract(0, 0.5, 0); // checking block under player WHEN player is jumping.
            Material type = under.getBlock().getType();

            if (!type.isSolid()) {
                continue;
            }

            return type == Material.ICE
                    || type == Material.BLUE_ICE
                    || type == Material.PACKED_ICE
                    || type == Material.FROSTED_ICE;
        }

        return false;
    }

    // handles fly hacks, or trys to... we just hope for best.
    private void checkFly(Player player, Location before, Location now) {
        UUID id = player.getUniqueId();
        String name = player.getName();

        Material feet = now.getBlock().getType();
        if (player.isInWater() || player.isInLava() || feet == Material.LADDER || feet == Material.VINE ) {
            airSecond.put(id, 0);
            return;
        }

        double dy = now.getY() - before.getY();
        boolean falling = dy < -0.1;

        Location lower = now.clone().subtract(0, 0.1, 0);
        boolean supported = player.collidesAt(lower);

        if (supported || falling) {
            airSecond.put(id, 0);
            return;
        }

        int inAir = airSecond.getOrDefault(id, 0) + 1;
        airSecond.put(id, inAir);

        if (inAir > 3) {
            getLogger().info(name + " Has been in air for " + inAir + " (NOT FALLING)");
            player.teleport(before);
            player.setVelocity((new Vector(0, 0, 0)));
            lastPlaces.put(id, before.clone());
        }
    }

    // Handles clean up, after players quits.
    @EventHandler
    public void playerLeft(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();

        lastPlaces.remove(id);
        fastSeconds.remove(id);
        airSecond.remove(id);
    }
}
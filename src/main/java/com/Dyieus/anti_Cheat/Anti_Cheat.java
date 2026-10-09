package com.Dyieus.anti_Cheat;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Anti_Cheat extends JavaPlugin implements Listener {

    // Save individual players data ( variable names describe what they save clearly )
    private final Map<UUID, playerData> dataMap = new ConcurrentHashMap<>();

    public playerData getData(UUID id) {
        return dataMap.get(id);
    }

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings().checkForUpdates(false);
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        PacketEvents.getAPI().init();
        PacketEvents.getAPI().getEventManager().registerListener(new movementPacketListener(this));

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new hitListener(this), this);

        new BukkitRunnable() {
            @Override
            public void run() {
                updateWorldInfo();
            }
        }.runTaskTimer(this, 1L, 1L);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        PacketEvents.getAPI().terminate();
    }

    // updates players information
    private void updateWorldInfo() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerData data = dataMap.computeIfAbsent(player.getUniqueId(), k -> new playerData());
            Location now = player.getLocation();
            Material feet = now.getBlock().getType();
            GameMode mode = player.getGameMode();

            boolean skipBoth = mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR
                    || player.isFlying() || player.isGliding() || player.isInsideVehicle()
                    || player.isInWater() || player.isInLava()
                    || feet == Material.LADDER || feet == Material.VINE;
            data.skipSpeed = skipBoth;

            data.skipFly = skipBoth
                    || player.hasPotionEffect(PotionEffectType.LEVITATION)
                    || player.hasPotionEffect(PotionEffectType.JUMP_BOOST);

            data.supported = player.collidesAt(now.clone().subtract(0, 0.1, 0));
            double limit = 0.5;
            if (speedBlock(now)) {
                limit *= 2.5;
            }
            PotionEffect speed = player.getPotionEffect(PotionEffectType.SPEED);
            if (speed != null) {
                limit *= 1 + 0.2 * (speed.getAmplifier() + 1);
            }
            data.maxSpeed = limit;

            if (data.supported && !data.skipSpeed) {
                data.safeSpot = now.clone();
            }
        }
    }

    // responsible for crafting flag message, and returning players to legit location
    public void punish(UUID id, String reason) {
        Bukkit.getScheduler().runTask(this, () -> {
            Player player = Bukkit.getPlayer(id);
            playerData data = dataMap.get(id);
            if (player == null || data == null || data.safeSpot == null) {
                return;
            }

            getLogger().info(player.getName() + " was flagged. Reason: " + reason);
            player.teleport(data.safeSpot);
            player.setVelocity(new Vector(0, 0 ,0));
        });
    }

    // checks for blocks that makes players run beyond limit
    public boolean speedBlock(Location spot) {
        Location under = spot.clone();
        for (int step = 0; step < 4; step++) {
            under.subtract(0, 0.5, 0);
            Material type = under.getBlock().getType();

            if (!type.isSolid()) {
                continue;
            }

            return type == Material.ICE
                    || type == Material.PACKED_ICE
                    || type == Material.FROSTED_ICE
                    || type == Material.BLUE_ICE;
        }

        return false;
    }

    // should prevent /tp and enderpearls, its just grace period.
    private void giveGrace(Player player, long millis) {
        playerData data = dataMap.get(player.getUniqueId());
        if (data != null) {
            data.graceUtil = System.currentTimeMillis() + millis;
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        giveGrace(event.getPlayer(), 1000);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        giveGrace(event.getPlayer(), 1000);
    }

    @EventHandler
    public void onVelocity(PlayerVelocityEvent event) {
        giveGrace(event.getPlayer(), 700);
    }

    @EventHandler
    public void playerLeft(PlayerQuitEvent event) {
        dataMap.remove(event.getPlayer().getUniqueId());
    }
}

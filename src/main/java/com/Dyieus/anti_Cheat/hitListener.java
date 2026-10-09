package com.Dyieus.anti_Cheat;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

public class hitListener implements Listener {
    public final Anti_Cheat plugin;

    public hitListener(Anti_Cheat plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        // did player attack?
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        // attacked player?
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        if (attacker.getGameMode() == GameMode.CREATIVE || attacker.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        Location eye = attacker.getEyeLocation();
        BoundingBox box = victim.getBoundingBox();

        double closestX = Math.max(box.getMinX(), Math.min(eye.getX(), box.getMaxX()));
        double closestY = Math.max(box.getMinY(), Math.min(eye.getY(), box.getMaxY()));
        double closestZ = Math.max(box.getMinZ(), Math.min(eye.getZ(), box.getMaxZ()));

        double dx = eye.getX() - closestX;
        double dy = eye.getY() - closestY;
        double dz = eye.getZ() - closestZ;

        double reach = Math.sqrt(dx * dx + dy * dy + dz * dz);
        playerData data = plugin.getData(attacker.getUniqueId());

        if (data == null) {
            return;
        }

        if (reach >= 6) {
            data.hitStrike++;
            event.setCancelled(true);
            plugin.getLogger().info(attacker.getName() + " has hit " + victim.getName() + " from " + reach + " blocks of distance");
        } else if (reach > 3.5) {
            data.hitStrike++;
            if (data.hitStrike >= 2) {
                event.setCancelled(true);
                plugin.getLogger().info(attacker.getName() + " has hit " + victim.getName() + " from " + reach + " blocks of distance");
            }
        } else {
            data.hitStrike = Math.max(0, data.hitStrike - 1);
        }

        // Above code checks only for REACH cheats
        // Belove code checks for kill aura cheats

        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            Vector look = eye.getDirection();
            Vector toward = victim.getEyeLocation().toVector().subtract(eye.toVector());

            if (toward.lengthSquared() > 0 && Math.toDegrees(look.angle(toward)) > 90 ) { // 90 is place holder for now
                event.setCancelled(true);
                plugin.getLogger().info(attacker.getName() + " has hit " + victim.getName() + " from " + Math.toDegrees(look.angle(toward)) + " angle");
            }
        }
    }
}

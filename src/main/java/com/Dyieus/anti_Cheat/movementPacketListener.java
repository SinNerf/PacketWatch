package com.Dyieus.anti_Cheat;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

import java.util.UUID;

public class movementPacketListener extends PacketListenerAbstract {
    public final Anti_Cheat plugin;

    public movementPacketListener(Anti_Cheat plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // record flying packets only
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            return;
        }

        User user = event.getUser();
        UUID id = user.getUUID();

        if (id == null) {
            return;
        }

        playerData data = plugin.getData(id);

        if (data == null) {
            return;
        }

        WrapperPlayClientPlayerFlying packet = new WrapperPlayClientPlayerFlying(event);

        if (!packet.hasPositionChanged()) {
            return;
        }

        double x = packet.getLocation().getX();
        double y = packet.getLocation().getY();
        double z = packet.getLocation().getZ();

        boolean inGrace = System.currentTimeMillis() < data.graceUtil;

        if (!data.hasLast || inGrace || data.exempt) {
            data.speedStrike = 0;
            data.airTick = 0;
            remember(data, x, y, z);
            return;
        }

        double dx = x - data.lastX;
        double dy = y - data.lastY;
        double dz = z - data.lastZ;

        checkSpeed(id, data, dx ,dz);
        checkFly(id, data, dy);

        remember(data, x, y, z);
    }

    // update players outdated info
    private void remember(playerData data, double x, double y, double z) {
        data.lastX = x;
        data.lastY = y;
        data.lastZ = z;
        data.hasLast = true;
    }

    // speed anti cheat checks
    private void checkSpeed(UUID id, playerData data, double dx, double dz) {
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        if (horizontal > data.maxSpeed) {
            data.speedStrike++;
        } else {
            data.speedStrike = Math.max(0, data.speedStrike - 1);
        }

        if (data.speedStrike >= 8) {
            data.speedStrike = 0;
            plugin.punish(id, "speed: " + horizontal + " block/tick");
        }
    }

    // fly anti cheat checks ( no need to pray this time )
    private void checkFly(UUID id, playerData data, double dy) {
        boolean notFalling = dy >= -0.01;

        if (data.supported || !notFalling) {
            data.airTick = 0;
        } else {
            data.airTick++;
        }

        if (data.airTick > 15) {
            data.airTick = 0;
            plugin.punish(id, "Fly (Not falling for 15 ticks)");
        }
    }
}

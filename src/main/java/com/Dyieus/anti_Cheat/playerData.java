package com.Dyieus.anti_Cheat;

import org.bukkit.Location;

public class playerData {

    // for packet thread
    public double lastX, lastY, lastZ;
    public boolean hasLast = false;
    public int speedStrike = 0;
    public int airTick = 0;

    // packet reads from below
    public volatile boolean exempt = false; // will be used for creative and ops
    public volatile boolean supported = false; // sanding on solid block?
    public volatile double maxSpeed = 0.5; // placeholder, it defines max allowed block per tick
    public volatile long graceUtil = 0;
    public volatile Location safeSpot = null; // saves players last legit location
}

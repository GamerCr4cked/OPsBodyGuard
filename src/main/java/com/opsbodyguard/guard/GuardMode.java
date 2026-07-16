package com.opsbodyguard.guard;

public enum GuardMode {
    AGGRESSIVE("Aggressive", "Attack anyone nearby"),
    NEUTRAL("Neutral", "Hit entity to attack"),
    PASSIVE("Passive", "Won't attack anyone"),
    HOSTILE("Hostile", "Only attack hostile mobs"),
    CUSTOM_ATTACK("Custom Attack", "Attack specific player");

    private final String displayName;
    private final String description;

    GuardMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}

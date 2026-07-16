package com.opsbodyguard.guard;

public enum PaymentType {
    PER_DAY("Per-Day Payment"),
    CUSTOM_DAYS("Custom Days Payment");

    private final String displayName;

    PaymentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

package com.opsbodyguard.guard;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BodyGuard {

    private UUID guardUUID;
    private UUID ownerUUID;
    private GuardMode mode;
    private PaymentType paymentType;
    private int daysRemaining;
    private long lastPaymentTime;
    private String customTargetName;
    private Location spawnLocation;
    private boolean orderActive = false; // Order toggle - listen to gstop/gattack commands
    private boolean orderStopped = false; // When gstop is used, pause attacking

    public BodyGuard(UUID ownerUUID, PaymentType paymentType, int days) {
        this.ownerUUID = ownerUUID;
        this.paymentType = paymentType;
        this.daysRemaining = days;
        this.mode = GuardMode.NEUTRAL;
        this.lastPaymentTime = System.currentTimeMillis();
        this.customTargetName = null;
    }

    // For loading from file
    public BodyGuard(UUID guardUUID, UUID ownerUUID, GuardMode mode, PaymentType paymentType,
                     int daysRemaining, long lastPaymentTime, String customTargetName) {
        this.guardUUID = guardUUID;
        this.ownerUUID = ownerUUID;
        this.mode = mode;
        this.paymentType = paymentType;
        this.daysRemaining = daysRemaining;
        this.lastPaymentTime = lastPaymentTime;
        this.customTargetName = customTargetName;
    }

    public UUID getGuardUUID() {
        return guardUUID;
    }

    public void setGuardUUID(UUID guardUUID) {
        this.guardUUID = guardUUID;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public Player getOwner() {
        return Bukkit.getPlayer(ownerUUID);
    }

    public GuardMode getMode() {
        return mode;
    }

    public void setMode(GuardMode mode) {
        this.mode = mode;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public int getDaysRemaining() {
        return daysRemaining;
    }

    public void setDaysRemaining(int daysRemaining) {
        this.daysRemaining = daysRemaining;
    }

    public void decrementDays() {
        this.daysRemaining--;
    }

    public long getLastPaymentTime() {
        return lastPaymentTime;
    }

    public void setLastPaymentTime(long lastPaymentTime) {
        this.lastPaymentTime = lastPaymentTime;
    }

    public String getCustomTargetName() {
        return customTargetName;
    }

    public void setCustomTargetName(String customTargetName) {
        this.customTargetName = customTargetName;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    public IronGolem getEntity() {
        if (guardUUID == null) return null;
        var entity = Bukkit.getEntity(guardUUID);
        if (entity instanceof IronGolem golem) {
            return golem;
        }
        return null;
    }

    public boolean isSpawned() {
        return guardUUID != null && getEntity() != null;
    }

    public boolean isOrderActive() {
        return orderActive;
    }

    public void setOrderActive(boolean orderActive) {
        this.orderActive = orderActive;
    }

    public boolean isOrderStopped() {
        return orderStopped;
    }

    public void setOrderStopped(boolean orderStopped) {
        this.orderStopped = orderStopped;
    }

    public boolean isPerDayPayment() {
        return paymentType == PaymentType.PER_DAY;
    }

    public boolean shouldPayToday() {
        if (paymentType != PaymentType.PER_DAY) return false;
        long dayInMillis = 24 * 60 * 60 * 1000L;
        return System.currentTimeMillis() - lastPaymentTime >= dayInMillis;
    }
}

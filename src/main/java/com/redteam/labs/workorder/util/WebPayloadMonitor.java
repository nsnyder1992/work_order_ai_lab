package com.redteam.labs.workorder.util;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class WebPayloadMonitor {

    private static final int MAX_ATTEMPTS = 5;
    private static final int[] LOCKOUT_INTERVALS = {5, 10, 15, 30, 60}; // in minutes

    private static class UserStatus {
        int attemptCount = 0;
        int lockoutLevel = 0;
        Instant lastAttempt = Instant.now();
        Instant lockoutUntil = Instant.MIN;
    }

    private static final Map<String, UserStatus> userStatusMap = new HashMap<>();

    public static synchronized boolean isLocked(String username) {
        UserStatus status = userStatusMap.get(username);
        if (status == null) return false;

        Instant now = Instant.now();
        return status.lockoutUntil.isAfter(now);
    }
    
    public static synchronized int strikeOutCount(String username) {
        return userStatusMap.getOrDefault(username, new UserStatus()).attemptCount;
    }

    public static synchronized String registerAttackAttempt(String username) {
        UserStatus status = userStatusMap.computeIfAbsent(username, k -> new UserStatus());

        if (isLocked(username)) {
            long remaining = status.lockoutUntil.getEpochSecond() - Instant.now().getEpochSecond();
            return "🚫 Your account is locked for suspicious input patterns. Try again in " + (remaining / 60 + 1) + " minutes.";
        }

        status.attemptCount++;
        status.lastAttempt = Instant.now();

        if (status.attemptCount >= MAX_ATTEMPTS) {
            int level = Math.min(status.lockoutLevel, LOCKOUT_INTERVALS.length - 1);
            int lockDuration = LOCKOUT_INTERVALS[level];

            status.lockoutUntil = Instant.now().plusSeconds(lockDuration * 60L);
            status.lockoutLevel++;
            status.attemptCount = 0;

            return "🚫 Web application firewall triggered multiple times. Your account has been locked for " + lockDuration + " minutes.";
        }

        return "⚠️ Suspicious activity detected. Further attempts may result in a temporary lockout.";
    }

    public static synchronized void reset(String username) {
        UserStatus status = userStatusMap.get(username);
        
        if (status == null) return;
        
        Instant now = Instant.now();
        Instant lastAttempt = (status != null) ? status.lastAttempt : now;
        
        if (now.isAfter(lastAttempt.plusSeconds(300))) { // 5 minutes
            // Reset if last attempt was more than 5 minutes ago
            userStatusMap.remove(username);
        }
    }
}

package com.redteam.labs.workorder.util;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class PromptInjectionMonitor
{
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

    public static synchronized String registerPromptInjection(String username) {
        UserStatus status = userStatusMap.computeIfAbsent(username, k -> new UserStatus());

        if (isLocked(username)) {
            long remaining = status.lockoutUntil.getEpochSecond() - Instant.now().getEpochSecond();
            return "Your account is currently locked for prompt injection attempts. Try again in " + (remaining / 60 + 1) + " minutes.";
        }

        status.attemptCount++;
        status.lastAttempt = Instant.now();

        if (status.attemptCount >= MAX_ATTEMPTS) {
            int lockoutLevel = Math.min(status.lockoutLevel, LOCKOUT_INTERVALS.length - 1);
            int duration = LOCKOUT_INTERVALS[lockoutLevel];
            status.lockoutUntil = Instant.now().plusSeconds(duration * 60L);
            status.attemptCount = 0;
            status.lockoutLevel++;

            return "🚫 Your account has been locked due to repeated prompt injection attempts. Try again in " + duration + " minutes.";
        }

        return "⚠️ We do not allow prompt injection techniques. You will be locked out of your account if you continue. " +
               "If you didn't mean to use a prompt injection technique, please rephrase your prompt.";
    }

    public static synchronized void reset(String username) {
        UserStatus status = userStatusMap.get(username);
        if (status == null) return;
        
        Instant now = Instant.now();
        if (now.isAfter(status.lastAttempt.plusSeconds(60 * 5))) { //5 min
            userStatusMap.remove(username);
        }
        

    }
}

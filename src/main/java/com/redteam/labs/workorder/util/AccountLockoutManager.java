package com.redteam.labs.workorder.util;

import java.util.concurrent.*;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

public class AccountLockoutManager {

    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_LOCKOUT_MINUTES = 60;

    // Maps usernames to failed attempt count
    private final ConcurrentHashMap<String, Integer> failedAttempts = new ConcurrentHashMap<>();

    // Maps usernames to lockout expiration timestamps
    private final ConcurrentHashMap<String, Long> lockoutExpirations = new ConcurrentHashMap<>();
    
    public boolean isLockedOut(String username) {
        Long unlockTime = lockoutExpirations.get(username);
        if (unlockTime == null) return false;
        if (System.currentTimeMillis() > unlockTime) {
            // Lockout expired
            lockoutExpirations.remove(username);
            failedAttempts.remove(username);
            return false;
        }
        return true;
    }

    public long getRemainingLockoutMillis(String username) {
        Long unlockTime = lockoutExpirations.get(username);
        return unlockTime == null ? 0 : Math.max(0, unlockTime - System.currentTimeMillis());
    }

    public void registerFailedAttempt(String username) {
        int attempts = failedAttempts.getOrDefault(username, 0) + 1;
        failedAttempts.put(username, attempts);

        if (attempts >= MAX_ATTEMPTS) {
            int lockoutMinutes = Math.min((attempts - MAX_ATTEMPTS + 1) * 5, MAX_LOCKOUT_MINUTES);
            long lockoutUntil = System.currentTimeMillis() + (lockoutMinutes * 60_000L);
            lockoutExpirations.put(username, lockoutUntil);
        }
    }

    public void reset(String username) {
        failedAttempts.remove(username);
        lockoutExpirations.remove(username);
    }

}
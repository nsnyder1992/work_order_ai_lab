package com.redteam.labs.workorder.util;

import java.util.concurrent.*;
import java.time.*;

public class AiTokenQuotaManager {

    private static final int DAILY_QUOTA = 2000;

    static class TokenUsage {
        int used;
        LocalDate date;

        TokenUsage() {
            this.used = 0;
            this.date = LocalDate.now();
        }
    }

    private final ConcurrentHashMap<String, TokenUsage> usageMap = new ConcurrentHashMap<>();

    public synchronized boolean canUseTokens(String username, int tokensRequested) {
        TokenUsage usage = usageMap.computeIfAbsent(username, k -> new TokenUsage());

        // Reset usage if it's a new day
        if (!usage.date.equals(LocalDate.now())) {
            usage.used = 0;
            usage.date = LocalDate.now();
        }

        return usage.used + tokensRequested <= DAILY_QUOTA;
    }

    public synchronized void recordUsage(String username, int tokensUsed) {
        TokenUsage usage = usageMap.computeIfAbsent(username, k -> new TokenUsage());

        // Reset if date changed
        if (!usage.date.equals(LocalDate.now())) {
            usage.used = 0;
            usage.date = LocalDate.now();
        }

        usage.used += tokensUsed;
    }

    public synchronized int getRemainingQuota(String username) {
        TokenUsage usage = usageMap.getOrDefault(username, new TokenUsage());
        if (!usage.date.equals(LocalDate.now())) {
            return DAILY_QUOTA;
        }
        return DAILY_QUOTA - usage.used;
    }
}

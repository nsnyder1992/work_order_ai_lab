package com.redteam.labs.workorder.filters;

import java.io.IOException;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.redteam.labs.workorder.model.User;
import com.redteam.labs.workorder.util.PromptInjectionMonitor;

import org.apache.commons.text.similarity.LevenshteinDistance;


@WebFilter("/jsp/chat") // Apply filter to all requests
public class PromptInjectionFilter implements javax.servlet.Filter
{
    private static final LevenshteinDistance distance = new LevenshteinDistance();
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        HttpServletRequest httpRequest = (HttpServletRequest) request;        
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession();
        User user = (User) session.getAttribute("user");
        if (user == null) {
            httpResponse.sendRedirect("login.jsp");
        }
        
        String input = httpRequest.getParameter("question");
        if (input != null && isPotentialPromptInjection(input)) {
            String username = (user != null) ? user.getUsername() : "anonymous";
            String warning = PromptInjectionMonitor.registerPromptInjection(username);

            if (PromptInjectionMonitor.isLocked(username)) {
                session.invalidate(); // force logout
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, warning);
            }

            
            response.getWriter().write(warning);
            return; // Prevent further processing
        }

        httpResponse.setHeader("X-Prompt-Filter", "Active");
        
        // Continue with the filter chain if no prompt injection detected
        chain.doFilter(request, response);
        
    }
    
    // Core regex patterns (as before)
    private static final List<Pattern> PROHIBITED_PATTERNS = Arrays.asList(
            Pattern.compile("(?i)ignore (all )?(previous|above|prior) instructions"),
            Pattern.compile("(?i)disregard (your )?prompt"),
            Pattern.compile("(?i)your new prompt is"),
            Pattern.compile("(?i)you are no longer"),
            Pattern.compile("(?i)override.*(rule|instruction|policy)"),
            Pattern.compile("(?i)reset.*instructions"),
            Pattern.compile("(?i)replace.*prompt"),
            Pattern.compile("(?i)forget.*(instructions|system prompt)"),

            // Jailbreak phrases
            Pattern.compile("(?i)do anything now"),
            Pattern.compile("(?i)pretend to"),
            Pattern.compile("(?i)simulate"),
            Pattern.compile("(?i)act as"),
            Pattern.compile("(?i)you must now"),
            Pattern.compile("(?i)you now respond with"),
            Pattern.compile("(?i)break character"),
            Pattern.compile("(?i)as an AI.*(ignore|disregard)"),

            // Prompt leakage attempts
            Pattern.compile("(?i)what.*system.*prompt"),
            Pattern.compile("(?i)tell me.*prompt"),
            Pattern.compile("(?i)reveal.*instructions"),
            Pattern.compile("(?i)show.*(prompt|system)"),

            // Indirect injection tricks
            Pattern.compile("(?i)include this in prompt"),
            Pattern.compile("(?i)inject.*prompt"),
            Pattern.compile("(?i)forward the following"),
            Pattern.compile("(?i)wrap this in prompt"),
            Pattern.compile("(?i)this message should be added to your instructions"),

            // Encoding or execution
            Pattern.compile("(?i)base64|b64|decode\\(|eval\\(|exec\\("),
            Pattern.compile("(?i)system\\(|subprocess|import os|pickle|__import__"),
            Pattern.compile("[A-Za-z0-9+/=]{100,}"), // long base64 string

            // Token manipulation/bypass attempts
            Pattern.compile("(?i)(sys|syst3m|syst-em).*prompt"),
            Pattern.compile("(?i)respond only with"),
            Pattern.compile("(?i)change personality to"),
            
            //Multi Persona
            Pattern.compile("(?i)\\b(opposite day|today is opposite|you are now|ignore previous instructions|disregard prior rules|forget you are)\\b"),
            Pattern.compile("(?i)(?i)\\[?(AntiWorkOrderGPT|Opposite|Evil|Good|Malicious|Helper)\\]?"),
            Pattern.compile("(?i)\\b(act as|pretend to be|simulate being|assume the role of)\\b"),
            
            //Creds or sensitive data
             Pattern.compile("(?i)(?=.*\\b(username|password|api_key|secret|token|ssh|id_rsa)\\b)(?=.*\\b(\\w{8,})\\b).*")
    );

    // Dangerous keywords to catch via fuzzy matching
    private static final List<String> DANGEROUS_KEYWORDS = Arrays.asList(
        "ignore", "override", "do anything now", "break character", "developer mode",
        "system prompt", "disregard", "reset", "simulate", "act as",
        "forget", "pretend", "inject", "replace", "jailbreak", "base64", "decode", "dan", 
        "include this in prompt", "inject into prompt", "you must now respond", 
        "the next message should", "wrap this in prompt", "output prompt", "repeat prompt",
        "forward the following", "tell me what the system prompt says", "bypass", "I am the owner of",
        "opposite day", "AntiWorkOrder"
    );

    // Levenshtein threshold
    private static final int LEVENSHTEIN_THRESHOLD = 2;

    /**
     * Entry point to check for prompt injection.
     */
    public static boolean isPotentialPromptInjection(String input) {
        if (input == null || input.isEmpty()) return false;

        String normalized = normalizeInput(input);

        try {
            // Check direct regex matches
            for (Pattern pattern : PROHIBITED_PATTERNS) {
                Matcher matcher = pattern.matcher(input);
                if (matcher.find()) return true;
            }
        } catch (Exception e) {
            System.err.println("Regex matching failed: " + e.getMessage());
        }

        // Token-level fuzzy match
        for (String keyword : DANGEROUS_KEYWORDS) {
            if (isFuzzyMatch(normalized, keyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Normalize input to minimize obfuscation tricks.
     */
    private static String normalizeInput(String input) {
        String temp = Normalizer.normalize(input, Normalizer.Form.NFD);
        temp = temp.replaceAll("\\p{M}", ""); // remove accents
        temp = temp.toLowerCase();
        temp = temp.replaceAll("[^a-z0-9 ]", ""); // remove punctuation
        temp = temp.replaceAll("\\s+", " ");     // collapse whitespace
        return temp.trim();
    }

    /**
     * Levenshtein match check for fuzziness.
     */
    private static boolean isFuzzyMatch(String input, String keyword) {
        if (distance.apply(input, keyword) <= LEVENSHTEIN_THRESHOLD)
            return true;
        return false;
    }

    /**
     * Optional: sanitize or block malicious input.
     */
    public static String sanitizeMessage(String input) {
        return isPotentialPromptInjection(input)
            ? "[⚠ Message blocked for security reasons.]"
            : input;
    }

}

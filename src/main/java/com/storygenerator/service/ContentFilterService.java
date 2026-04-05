package com.storygenerator.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class ContentFilterService {

    private static final Set<String> BLOCKED_WORDS = new HashSet<>(Arrays.asList(
        "violence", "violent", "kill", "murder", "blood", "death", "die",
        "weapon", "gun", "knife", "sword", "fight", "war", "hate",
        "scary", "horror", "terror", "nightmare", "demon", "devil",
        "drug", "alcohol", "smoke", "gambling",
        "bully", "abuse", "cruel", "torture"
    ));

    private static final Pattern UNSAFE_PATTERN = Pattern.compile(
        "\\b(" + String.join("|", BLOCKED_WORDS) + ")\\b",
        Pattern.CASE_INSENSITIVE
    );

    public boolean isInputSafe(String input) {
        if (input == null || input.trim().isEmpty()) {
            return true;
        }
        return !UNSAFE_PATTERN.matcher(input).find();
    }

    public String sanitizeInput(String input) {
        if (input == null) return "";
        // Remove special characters but keep unicode for Hindi support
        String sanitized = input.replaceAll("[<>\"'&;(){}\\[\\]\\\\]", "");
        return sanitized.trim();
    }

    public String filterStoryContent(String content) {
        if (content == null) return "";
        // Replace any potentially unsafe words that might slip through
        String filtered = content;
        for (String word : BLOCKED_WORDS) {
            filtered = filtered.replaceAll("(?i)\\b" + Pattern.quote(word) + "\\b", "✨magic✨");
        }
        return filtered;
    }

    public String getUnsafeInputMessage(String language) {
        return switch (language) {
            case "hi" -> "कृपया बच्चों के अनुकूल शब्दों का उपयोग करें! 🌟 आइए एक अच्छी और मज़ेदार कहानी बनाएं!";
            case "hinglish" -> "Please bacchon ke liye achhe words use karo! 🌟 Chalo ek mast aur fun story banaate hain!";
            case "bn" -> "দয়া করে শিশু-বান্ধব শব্দ ব্যবহার করুন! 🌟 চলো একটা সুন্দর আর মজার গল্প তৈরি করি!";
            default -> "Please use child-friendly words! 🌟 Let's create a nice and fun story together!";
        };
    }
}

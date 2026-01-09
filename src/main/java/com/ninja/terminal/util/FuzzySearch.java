package com.ninja.terminal.util;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;

public class FuzzySearch {

    public static class MatchResult {
        private final String item;
        private final int score;
        private final List<Integer> matchedIndices;

        public MatchResult(String item, int score, List<Integer> matchedIndices) {
            this.item = item;
            this.score = score;
            this.matchedIndices = matchedIndices;
        }

        public String getItem() { return item; }
        public int getScore() { return score; }
        public List<Integer> getMatchedIndices() { return matchedIndices; }
    }

    public static List<MatchResult> search(String query, List<String> items) {
        if (query == null || query.isEmpty()) {
            return items.stream()
                    .map(item -> new MatchResult(item, 0, Collections.emptyList()))
                    .toList();
        }

        List<MatchResult> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        for (String item : items) {
            MatchResult result = fuzzyMatch(lowerQuery, item.toLowerCase());
            if (result != null) {
                results.add(new MatchResult(item, result.getScore(), result.getMatchedIndices()));
            }
        }

        results.sort(Comparator.comparingInt(MatchResult::getScore).reversed());
        return results;
    }

    private static MatchResult fuzzyMatch(String query, String text) {
        if (query.isEmpty()) {
            return new MatchResult(text, 0, Collections.emptyList());
        }

        List<Integer> matchedIndices = new ArrayList<>();
        int queryIndex = 0;
        int score = 0;
        int consecutiveMatches = 0;

        for (int i = 0; i < text.length() && queryIndex < query.length(); i++) {
            if (text.charAt(i) == query.charAt(queryIndex)) {
                matchedIndices.add(i);
                queryIndex++;
                consecutiveMatches++;
                score += 10;
                if (consecutiveMatches > 1) {
                    score += consecutiveMatches * 2;
                }
            } else {
                consecutiveMatches = 0;
            }
        }

        if (queryIndex == query.length()) {
            return new MatchResult(text, score, matchedIndices);
        }

        return null;
    }

    public static String highlightMatch(String text, List<Integer> matchedIndices) {
        if (matchedIndices.isEmpty()) {
            return text;
        }

        StringBuilder highlighted = new StringBuilder();
        int lastMatchIndex = -1;

        for (int i = 0; i < text.length(); i++) {
            if (matchedIndices.contains(i)) {
                if (i != lastMatchIndex + 1) {
                    highlighted.append("|");
                }
                highlighted.append(text.charAt(i));
                lastMatchIndex = i;
            } else {
                if (lastMatchIndex >= 0 && i == lastMatchIndex + 1) {
                    highlighted.append("|");
                }
                highlighted.append(text.charAt(i));
            }
        }

        if (lastMatchIndex == text.length() - 1) {
            highlighted.append("|");
        }

        return highlighted.toString();
    }
}

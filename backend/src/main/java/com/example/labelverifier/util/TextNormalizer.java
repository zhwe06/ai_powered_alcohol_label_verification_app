package com.example.labelverifier.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class TextNormalizer {

    private TextNormalizer() {
    }

    /**
     * Normalizes text so that differences agents would ignore
     * (case, spacing, curly vs straight quotes) do not cause mismatches.
     */
    public static String forComparison(String text) {
        if (text == null) {
            return "";
        }

        return Normalizer.normalize(text, Normalizer.Form.NFKC)
                .replaceAll("[\u2018\u2019\u02BC`]", "'")
                .replaceAll("[\u201C\u201D]", "\"")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Returns true if {@code phrase} appears in {@code text} as whole words,
     * so "ace" does not match inside "space".
     */
    public static boolean containsPhrase(String text, String phrase) {
        String normalizedText = forComparison(text);
        String normalizedPhrase = forComparison(phrase);

        if (normalizedPhrase.isEmpty()) {
            return false;
        }

        Pattern pattern = Pattern.compile(
                "(?<![a-z0-9])" + Pattern.quote(normalizedPhrase) + "(?![a-z0-9])");

        return pattern.matcher(normalizedText).find();
    }
}

package com.fixzone.fixzon_backend.util;

import java.security.SecureRandom;

/**
 * Utility class for generating secure, unique, random passwords.
 * Ensures generated passwords meet password complexity standards:
 * contains uppercase, lowercase, digit, and special character.
 */
public class PasswordGenerator {
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ"; // O and I omitted to avoid confusion
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz"; // l omitted to avoid confusion
    private static final String DIGITS = "23456789"; // 0 and 1 omitted to avoid confusion
    private static final String SPECIAL = "!@#$%&*";
    private static final String ALL_CHARS = UPPER + LOWER + DIGITS + SPECIAL;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generates a secure, readable, random unique password.
     *
     * @param length Desired length (minimum 8)
     * @return Generated password string
     */
    public static String generateUniquePassword(int length) {
        if (length < 8) {
            length = 10;
        }

        StringBuilder password = new StringBuilder(length);
        // Ensure at least one uppercase, lowercase, digit, and special character
        password.append(UPPER.charAt(RANDOM.nextInt(UPPER.length())));
        password.append(LOWER.charAt(RANDOM.nextInt(LOWER.length())));
        password.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        password.append(SPECIAL.charAt(RANDOM.nextInt(SPECIAL.length())));

        for (int i = 4; i < length; i++) {
            password.append(ALL_CHARS.charAt(RANDOM.nextInt(ALL_CHARS.length())));
        }

        // Shuffle the characters thoroughly using Fisher-Yates algorithm
        char[] array = password.toString().toCharArray();
        for (int i = array.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }

        return new String(array);
    }
}

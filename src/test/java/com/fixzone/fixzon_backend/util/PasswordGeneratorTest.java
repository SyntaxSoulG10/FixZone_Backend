package com.fixzone.fixzon_backend.util;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordGeneratorTest {

    @Test
    void testPasswordGenerationCriteria() {
        for (int i = 0; i < 50; i++) {
            String password = PasswordGenerator.generateUniquePassword(10);
            assertNotNull(password);
            assertEquals(10, password.length());

            boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
            boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
            boolean hasDigit = password.chars().anyMatch(Character::isDigit);
            boolean hasSpecial = password.chars().anyMatch(ch -> "!@#$%&*".indexOf(ch) >= 0);

            assertTrue(hasUpper, "Password must have uppercase character: " + password);
            assertTrue(hasLower, "Password must have lowercase character: " + password);
            assertTrue(hasDigit, "Password must have digit: " + password);
            assertTrue(hasSpecial, "Password must have special character: " + password);
        }
    }

    @Test
    void testPasswordUniqueness() {
        Set<String> passwords = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            String pwd = PasswordGenerator.generateUniquePassword(10);
            assertFalse(passwords.contains(pwd), "Passwords generated should be unique");
            passwords.add(pwd);
        }
    }
}

package com.lab.atlasmentor.util;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class PasswordGenerator {
    
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*";
    private static final String ALL_CHARS = UPPER + LOWER + DIGITS + SPECIAL;
    
    private static final SecureRandom random = new SecureRandom();
    
    public static String generateRandomPassword(int length) {
        if (length < 8) {
            length = 8; // Minimum password length
        }
        
        // Ensure at least one character from each category
        List<Character> passwordChars = Stream.of(
                UPPER.charAt(random.nextInt(UPPER.length())),
                LOWER.charAt(random.nextInt(LOWER.length())),
                DIGITS.charAt(random.nextInt(DIGITS.length())),
                SPECIAL.charAt(random.nextInt(SPECIAL.length()))
        ).collect(Collectors.toList());
        
        // Fill the rest with random characters
        IntStream.range(4, length).forEach(i -> 
            passwordChars.add(ALL_CHARS.charAt(random.nextInt(ALL_CHARS.length())))
        );
        
        // Shuffle the characters
        Collections.shuffle(passwordChars);
        
        return passwordChars.stream()
                .map(String::valueOf)
                .collect(Collectors.joining());
    }
    
    public static String generateRandomPassword() {
        return generateRandomPassword(12); // Default 12 characters
    }
}

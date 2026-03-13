package com.example.a2hauto.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AuthValidatorTest {

    @Test
    public void normalizePhone_removesWhitespace() {
        assertEquals("0987654321", AuthValidator.normalizePhone(" 0987 654 321 "));
    }

    @Test
    public void isValidPhone_acceptsNineToElevenDigits() {
        assertTrue(AuthValidator.isValidPhone("0987654321"));
        assertTrue(AuthValidator.isValidPhone("09123456789"));
        assertFalse(AuthValidator.isValidPhone("12345678"));
        assertFalse(AuthValidator.isValidPhone("09A8765432"));
    }

    @Test
    public void isValidFullName_requiresAtLeastTwoCharacters() {
        assertTrue(AuthValidator.isValidFullName("Minh Anh"));
        assertFalse(AuthValidator.isValidFullName(" "));
        assertFalse(AuthValidator.isValidFullName("A"));
    }

    @Test
    public void isValidPassword_requiresUpperLowerDigitAndLength() {
        assertTrue(AuthValidator.isValidPassword("StrongPass1"));
        assertFalse(AuthValidator.isValidPassword("weakpass1"));
        assertFalse(AuthValidator.isValidPassword("WEAKPASS1"));
        assertFalse(AuthValidator.isValidPassword("WeakPass"));
        assertFalse(AuthValidator.isValidPassword("Aa1"));
    }
}


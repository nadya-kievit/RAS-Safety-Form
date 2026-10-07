package com.ras.safetyform.service;

import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 20;

    public void validate(String password) {
        int length = password.codePointCount(0, password.length());
        if (length < MIN_LENGTH) {
            throw new InvalidRequestException(
                    "Password must be at least " + MIN_LENGTH + " characters");
        }
        if (length > MAX_LENGTH) {
            throw new InvalidRequestException(
                    "Password must be no more than " + MAX_LENGTH + " characters");
        }
        if (password.codePoints().noneMatch(codePoint -> codePoint >= '0' && codePoint <= '9')) {
            throw new InvalidRequestException("Password must contain at least one number");
        }
        boolean hasSymbol = password.codePoints().anyMatch(codePoint ->
                !Character.isLetterOrDigit(codePoint)
                        && !Character.isWhitespace(codePoint));
        if (!hasSymbol) {
            throw new InvalidRequestException(
                    "Password must contain at least one non-alphanumeric character");
        }
    }
}

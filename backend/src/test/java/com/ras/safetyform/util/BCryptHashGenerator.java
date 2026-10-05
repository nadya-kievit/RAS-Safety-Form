package com.ras.safetyform.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class BCryptHashGenerator {

    private BCryptHashGenerator() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Provide at least one password to hash");
        }

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        if ("--verify".equals(args[0])) {
            if (args.length < 3 || args.length % 2 == 0) {
                throw new IllegalArgumentException(
                        "Provide one or more password and hash pairs after --verify");
            }
            for (int index = 1; index < args.length; index += 2) {
                if (!passwordEncoder.matches(args[index], args[index + 1])) {
                    throw new IllegalArgumentException(
                            "Password does not match the BCrypt hash");
                }
            }
            System.out.println("All passwords match their BCrypt hashes");
            return;
        }

        for (String password : args) {
            String hash = passwordEncoder.encode(password);
            if (!passwordEncoder.matches(password, hash)) {
                throw new IllegalStateException("Generated hash did not match its password");
            }
            System.out.println(hash);
        }
    }
}

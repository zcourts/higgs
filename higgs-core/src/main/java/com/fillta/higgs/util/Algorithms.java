package com.fillta.higgs.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Algorithms {
    public static String sha256(String str) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(str.getBytes());
            return new String(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}

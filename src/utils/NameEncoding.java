/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package utils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

/**
 *
 * @author uceeftu
 */
public class NameEncoding {

    public static BigInteger stringToBigInteger(String name) {

        String paddedName = name;

        if (name.length() < 64) {
            StringBuilder paddedString = new StringBuilder(name);
            while (paddedString.length() < 64) {
                paddedString.append(' ');
            }

            paddedName = paddedString.toString();

        }

        return new BigInteger(1, paddedName.toLowerCase().getBytes(StandardCharsets.UTF_8));

    }
}

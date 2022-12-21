/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import java.math.BigInteger;

/**
 *
 * @author andreads
 */
public class TestConvert {

    public static void main(String[] args) {
        String v = "0x18f3";
        System.out.println("val: " + v);
        BigInteger b = new BigInteger(v.replace("0x",""), 16);
        System.out.println("Big int (10): " + b.toString(10));
        System.out.println("Big int (16): " + b.toString());
    }
}

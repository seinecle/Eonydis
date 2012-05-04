/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eonydis;

import java.util.HashMap;
import org.joda.time.LocalDate;

/**
 *
 * @author Clement
 */
class SpellString {

    public final HashMap<String, String> stringValues;
    public final LocalDate date;

    public SpellString(LocalDate right, HashMap<String, String> stringValues) {
        this.stringValues = stringValues;
        this.date = right;
    }

}

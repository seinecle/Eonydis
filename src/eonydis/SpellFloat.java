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
class SpellFloat {

    public final HashMap<String, Float> floatValues;
    public final LocalDate date;

    public SpellFloat(LocalDate right, HashMap<String, Float> floatValues) {
        this.floatValues = floatValues;
        this.date = right;
    }

}

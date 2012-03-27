/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eonydis;

import org.joda.time.LocalDate;

/**
 *
 * @author Clement
 */
class Spell {

    public final Float values[];
    public final LocalDate date;

    public Spell(LocalDate right,Float[] averageValues) {
        
        this.values = averageValues;
        this.date = right;
        

    }
    
    
    
}

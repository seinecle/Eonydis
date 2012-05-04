/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eonydis;

import com.google.common.collect.TreeMultiset;
import java.util.Iterator;
import levallois.clement.utils.PairDates;
import org.joda.time.LocalDate;

/**
 *
 * @author C. Levallois
 */
public class ConsecutiveSpellsCleaner {



    
    
    static TreeMultiset<PairDates> doCleaning(TreeMultiset<LocalDate> setMultiDates) {
        
        Iterator<LocalDate> iteratorDates = setMultiDates.elementSet().iterator();
        TreeMultiset<PairDates> setStartEndDates = TreeMultiset.create();
        boolean newSpell = true;
        boolean newSpell2 = false;
        int counterOfIterations = 0;
        LocalDate dtStart = null;
        LocalDate dtLastCurrDate = null;
        int counterDates = 0;


        while (iteratorDates.hasNext()) {
            
            counterOfIterations++;
            if (newSpell) {
                dtStart = iteratorDates.next();
//                System.out.println("printing the next date in the deletion function: "+dtStart);
                dtLastCurrDate = dtStart;
                counterDates = 0;
            }

            if (newSpell2) {
                dtLastCurrDate = dtStart;
                counterDates = 0;
                newSpell2 = false;
            }

            if (iteratorDates.hasNext()) {
                dtLastCurrDate = iteratorDates.next();
                counterOfIterations++;
                counterDates++;
            } else {

                setStartEndDates.add(new PairDates(dtStart, dtLastCurrDate));
                continue;
            }


            if (!dtStart.plusDays(counterDates).equals(dtLastCurrDate)) {

                setStartEndDates.add(new PairDates(dtStart, dtStart.plusDays(counterDates - 1)));
                dtStart = dtLastCurrDate;
                newSpell2 = true;
                newSpell = false;
                if (!iteratorDates.hasNext()) {
                    setStartEndDates.add(new PairDates(dtStart, dtStart));
                }

            } else {
                newSpell = false;
                if (counterOfIterations == setMultiDates.elementSet().size()) {
                    setStartEndDates.add(new PairDates(dtStart, dtStart.plusDays(counterDates)));
                }
                
            }

        }
   

        return setStartEndDates;
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eonydis;

import com.google.common.collect.TreeMultiset;
import java.util.*;
import levallois.clement.utils.Pair;
import levallois.clement.utils.PairDates;
import levallois.clement.utils.Triple;
import org.joda.time.LocalDate;

/**
 *
 * @author Clement
 */
public class WorkerThreadEdges implements Runnable {

    private final int edgeCounter;
    private final int transactionCounter;
    private final Triple<Transaction, Pair<String, String>, LocalDate> refTransaction;
    private final Pair<String, String> refNodesPair;
    private int counterDates = 0;
    private boolean newSpell2 = false;
    private Float[] emptyValues = new Float[0];
    private Float currValue;

    public WorkerThreadEdges(int edgeCounter, int transactionCounter, Triple currTrans) {

        // this constructor defines:

        //an edge counter
        this.edgeCounter = edgeCounter;

        // the line number of the current transaction on which we were iterating at the moment the edge creation was launched
        this.transactionCounter = transactionCounter;

        //the full transaction of which we are currently iterating (made a triple: current transaction, pair of banks, date)
        this.refTransaction = currTrans;

        //in particular, we retain the pair of bank of this transaction. This pair of bank is a NEW ONE hence defines a new edge
        this.refNodesPair = refTransaction.getMiddle();
        run();


    }

    @Override
    public void run() {

        LinkedList<Spell> listSpells = new LinkedList();

        //we start a new iteration on the list of transactions. But we start the iteration at the current transaction up to the end of the file,
        //because we now the previous ones have been treated already
        ListIterator<Triple<HashMap<String, String>, Pair<String, String>, LocalDate>> it = Main.listTransactionsAndDates.listIterator(transactionCounter - 1);

        //this will record all the dates (several identical dates possible) when the pair of banks of the edge exists
        TreeMultiset<LocalDate> setMultiDates = TreeMultiset.create();

        //this will record the values corresponding to the multiple dates
        // since we have multiple attributes, WITHIN EACH TRANSACTION we will have to loop for as many edge attributes as the user has defined in Main.edgeAttributes
        HashMap<LocalDate, Float[]> setMultiValues = new HashMap();


        TreeMultiset<PairDates> setStartEndDates = TreeMultiset.create();


        // loop through all transactions, starting at the current one (see comment above)
        // this iteration produces a map of dates + sum of the corresponding values

        while (it.hasNext()) {

            Triple<HashMap<String, String>, Pair<String, String>, LocalDate> currTrans = it.next();
            Pair currPair = currTrans.getMiddle();

            //only if the current edge (pair of items) is present should the transaction be considered
            if (currPair.equals(refNodesPair)) {

                //beginning of the loop through edge attributes
                for (int i = 0; i < Main.edgeAttributes.length; i++) {

                    //This try-catch treats null values for attributes as zeros.
                    try {
                        currValue = Float.parseFloat(currTrans.getLeft().get(Main.edgeAttributes[i]));
                    } catch (NumberFormatException e) {
                        currValue = (float) 0;
                    }


                    //if the list of (dates, value) already contains the date of the current transaction
                    if (setMultiValues.containsKey(currTrans.getRight())) {

                        //retrieve the current value stored for the date
                        Float[] storedValue = setMultiValues.get(currTrans.getRight());


                        //These two lines treat null values for attributes as zeros.
                        if (storedValue[i] == null) {
                            storedValue[i] = (float) (0);
                        }

                        //add the currValue to the storedValue
                        storedValue[i] = storedValue[i] + currValue;

                        //reinput the new storedValue in the list of (dates,Values)
                        setMultiValues.put(currTrans.getRight(), storedValue);

                    } else {
                        Float[] attValues = new Float[Main.edgeAttributes.length];
                        attValues[i] = currValue;
                        setMultiValues.put(currTrans.getRight(), attValues);

                    }

                }

                if (Main.edgeAttributes.length == 0) {
                    setMultiValues.put(currTrans.getRight(), emptyValues);


                }

                //add the date to an ordered multiset. The edge creation will be based on an iteration of this (chronologically) ordered set
                setMultiDates.add(currTrans.getRight());


            }

        }

        //iterate through all the unique dates and their unique values
        Iterator<LocalDate> itSpells = setMultiValues.keySet().iterator();
        while (itSpells.hasNext()) {
            LocalDate spellDate = itSpells.next();

            //a spell is actually the spell itself + the associated value for the attributes of the edge at this date
            // the associated value
            for (int j = 0; j < Main.edgeAttributes.length; j++) {

                setMultiValues.get(spellDate)[j] = setMultiValues.get(spellDate)[j] / setMultiDates.count(spellDate);

            }

            //deals with the case when no edge attributes were selected by the user
            if (Main.edgeAttributes.length == 0) {
                Spell spell = new Spell(spellDate, emptyValues);
                listSpells.add(spell);
            }

            Spell spell = new Spell(spellDate, setMultiValues.get(spellDate));
            listSpells.add(spell);
        }





        StringBuilder oneEdgeAttValues = new StringBuilder();
        StringBuilder oneEdgeSpells = new StringBuilder();
        StringBuilder oneEdgeFull = new StringBuilder();

        oneEdgeAttValues.append("<attvalues>\n");

        Iterator<Spell> listSpellsIt = listSpells.iterator();


        listSpellsIt = listSpells.iterator();
        while (listSpellsIt.hasNext()) {

            Spell currSpell = listSpellsIt.next();
            for (int i = 0; i < currSpell.values.length; i++) {
                if (Main.edgeWeight != null) {
                    if (Main.edgeWeight.equals(Main.edgeAttributes[i])) {

                        oneEdgeAttValues.append("<attvalue for=\"weight\" value=\"").append(currSpell.values[i]).append("\" start=\"");

                    }
                } else {
                    oneEdgeAttValues.append("<attvalue for=\"").append(Main.edgeAttributes[i]).append("\" value=\"").append(currSpell.values[i]).append("\" start=\"");
                }

                oneEdgeAttValues.append(currSpell.date).append("\" ");
                oneEdgeAttValues.append("end=\"");
                oneEdgeAttValues.append(currSpell.date).append("\" ");
                oneEdgeAttValues.append("/>");
                oneEdgeAttValues.append("\n");
            }

        }


        // We now have a setMultiDates of all the dates when the two banks interact
        // This setMultiDates needs to be modified to delete the dates that are consecutive
        Iterator<LocalDate> iteratorDates = setMultiDates.elementSet().iterator();
        boolean newSpell = true;
        LocalDate dtStart = null;
        LocalDate dtLastCurrDate = null;




        while (iteratorDates.hasNext()) {

            if (newSpell) {
                dtStart = iteratorDates.next();
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
                counterDates = counterDates + 1;
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
            }

        }
        //!!!! iterates through all the dates for this node and creates corresponding spells
        Iterator<PairDates> iteratorStartEndDates = setStartEndDates.iterator();
        while (iteratorStartEndDates.hasNext()) {
            PairDates<LocalDate, LocalDate> currPair = iteratorStartEndDates.next();
            LocalDate startDate = currPair.getLeft();
            LocalDate endDate = currPair.getRight();
            oneEdgeSpells.append("<spell start=\"");
            oneEdgeSpells.append(startDate).append("\" ");
            oneEdgeSpells.append("end=\"");
            oneEdgeSpells.append(endDate).append("\" ");
            oneEdgeSpells.append("/>");
            oneEdgeSpells.append("\n");

        }//end of the iteration trough dates





        oneEdgeFull.append("<edge id=\"").append(edgeCounter).append("\" source=\"").append(refNodesPair.getLeft()).append("\" target=\"").append(refNodesPair.getRight()).append("\">\n");

        //this condition deals with the case when no edge attributes were selected by the user
        if (Main.edgeAttributes.length != 0) {
            oneEdgeFull.append(oneEdgeAttValues);
            oneEdgeFull.append("</attvalues>\n");
        }
        oneEdgeFull.append("<spells>\n");
        oneEdgeFull.append(oneEdgeSpells);
        oneEdgeFull.append("</spells>\n");
        oneEdgeFull.append("</edge>\n");

        Main.edges.append(oneEdgeFull);

//            Main.queue.add(oneEdgeFull);
//            //System.out.println(oneEdgeFull.toString());
//            oneEdgeAttValues = null;
//            oneEdgeSpells = null;
//            oneEdgeFull = null;
//            Main.flushQueue();




    }
}

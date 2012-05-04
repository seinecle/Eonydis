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
    private float currValueFloat;
    private String currValueString;
    private HashSet<String> setEdgeAttributes = new HashSet();

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

        LinkedList<SpellFloat> listSpellsFloat = new LinkedList();
        LinkedList<SpellString> listSpellsString = new LinkedList();

        //we start a new iteration on the list of transactions. But we start the iteration at the current transaction up to the end of the file,
        //because we now the previous ones have been treated already

        ListIterator<Triple<HashMap<String, String>, Pair<String, String>, LocalDate>> listTransactionsAndDatesIterator = Main.listTransactionsAndDates.listIterator(transactionCounter);

        //this will record all the dates (several identical dates possible) when the pair of agents of the edge exists
        TreeMultiset<LocalDate> setMultiDates = TreeMultiset.create();

        //this will record the values corresponding to the multiple dates
        // since we have multiple attributes, WITHIN EACH TRANSACTION we will have to loop for as many edge attributes as the user has defined in Main.edgeAttributes
        HashMap<LocalDate, HashMap<String, Float>> mapValuesFloat = new HashMap();
        HashMap<LocalDate, HashMap<String, String>> mapValuesString = new HashMap();


        TreeMultiset<PairDates> setStartEndDates;
        boolean oneStringAttributeDetected = false;

        // loop through all transactions, starting at the current one (see comment above)
        // this iteration produces a map of dates + (sum or average) of the corresponding values

        while (listTransactionsAndDatesIterator.hasNext()) {

            Triple<HashMap<String, String>, Pair<String, String>, LocalDate> currTrans = listTransactionsAndDatesIterator.next();
            Pair currPair = currTrans.getMiddle();
            LocalDate currDate = currTrans.getRight();
            HashMap<String, String> currAllFieldValues = currTrans.getLeft();

            System.out.println("currPair: " + currPair.getLeft() + " " + currPair.getRight());
            System.out.println("refNodesPair: " + refNodesPair.getLeft() + " " + refNodesPair.getRight());
            System.out.println("we are in the first loop");

            //only if the current edge (pair of items) is present should the transaction be considered
            if (currPair.equals(refNodesPair)) {


                //create an array of strings to record the string values of this transaction
                HashMap<String, String> stringValues = new HashMap();

                //creates a set of the edge attributes
                setEdgeAttributes.addAll(Arrays.asList(Main.edgeAttributes));


                //beginning of the loop through edge attributes

                Iterator<String> setEdgeAttributesIterator = setEdgeAttributes.iterator();

                while (setEdgeAttributesIterator.hasNext()) {
                    String currEdgeAttribute = setEdgeAttributesIterator.next();
                    System.out.println("in edge worker - curren edge attribute is " + currEdgeAttribute);

                    if (Main.stringAttributes.contains(currEdgeAttribute)) {

                        try {
                            currValueString = currAllFieldValues.get(currEdgeAttribute);
                        } catch (NullPointerException e) {
                            currValueString = "";
                        }
                        stringValues.put(currEdgeAttribute, currValueString);
                        oneStringAttributeDetected = true;
                        continue;


                    }

                    //end of the case STRING values. Beginning case FLOAT values
                    //This try-catch treats null values for attributes as zeros.

                    try {
                        currValueFloat = Float.parseFloat(currTrans.getLeft().get(currEdgeAttribute));
                    } catch (NumberFormatException e) {
                        currValueFloat = (float) 0;
                    }

                    HashMap<String, Float> storedFloatValues = new HashMap();

                    //if the list of (dates, value) already contains the date of the current transaction
                    if (mapValuesFloat.containsKey(currDate)) {

                        //retrieve the current value stored for the date
                        storedFloatValues = mapValuesFloat.get(currDate);


                        //These two lines treat null values for attributes as zeros.
                        if (storedFloatValues.get(currEdgeAttribute) == null) {
                            storedFloatValues.put(currEdgeAttribute, (float) 0);
                        }

                        //add the currValue to the storedValue
                        storedFloatValues.put(currEdgeAttribute, storedFloatValues.get(currEdgeAttribute) + currValueFloat);

                        //reinput the new storedValue in the list of (dates,Values)
                        mapValuesFloat.put(currDate, storedFloatValues);

                    } else {
                        storedFloatValues.put(currEdgeAttribute, currValueFloat);
                        mapValuesFloat.put(currDate, storedFloatValues);

                    }

                }

//                if (Main.edgeAttributes.length == 0) {
//                    setMultiValues.put(currTrans.getRight(), emptyValues);
//
//
//                }
                if (oneStringAttributeDetected) {
                    mapValuesString.put(currDate, stringValues);
                }

                //add the date to an ordered multiset. The edge creation will be based on an iteration of this (chronologically) ordered set
                setMultiDates.add(currDate);


            }

        }// end of loop through the list of all pairs <Node,Date>


        // ######################################################################

        // What we have now is:
        // HashMap<LocalDate, HashMap<String, Float>> mapValuesFloat = new HashMap();
        // HashMap<LocalDate, HashMap<String, String>> mapValuesString = new HashMap();

        // These two maps contain the set of Dates where the node appear, and for each date, the map of the attributes selected by the user.

        // We iterate through each of them to build spells. Honestly, not sure this is necessary, except that it helps deal with the averaging / summing case for Floats

        //Iterate through all the unique dates and their unique values for FLOAT

        Iterator<LocalDate> itSpells = mapValuesFloat.keySet().iterator();
        while (itSpells.hasNext()) {
            LocalDate spellDate = itSpells.next();

            // this applies the averaging of values appearing on the same date, or summing depending on the user's selection

            HashMap<String, Float> tempFloatValues = new HashMap();

            Iterator<Map.Entry<String, Float>> mapValuesFloatCurrDateIterator = mapValuesFloat.get(spellDate).entrySet().iterator();
            while (mapValuesFloatCurrDateIterator.hasNext()) {

                Map.Entry<String, Float> currEntry = mapValuesFloatCurrDateIterator.next();

                if (Main.doAverage) {
                    tempFloatValues.put(currEntry.getKey(), currEntry.getValue() / (float) setMultiDates.count(spellDate));
                } else {
                    tempFloatValues.put(currEntry.getKey(), currEntry.getValue());
                }
            }

            mapValuesFloat.put(spellDate, tempFloatValues);
            //deals with the case when no node attributes were selected by the user
//                if (nodeAttributes.length == 0) {
//                    SpellFloat spell = new SpellFloat(spellDate, emptyValues);
//                    listSpells.add(spell);
//                }

            SpellFloat spell = new SpellFloat(spellDate, mapValuesFloat.get(spellDate));
            listSpellsFloat.add(spell);
        }

        if (oneStringAttributeDetected) {
            itSpells = mapValuesString.keySet().iterator();
            while (itSpells.hasNext()) {
                LocalDate spellDate = itSpells.next();
//
//            //deals with the case when no edge attributes were selected by the user
//            if (Main.edgeAttributes.length == 0) {
//                SpellFloat spell = new SpellFloat(spellDate, emptyValues);
//                listSpells.add(spell);
//            }

                SpellString spellString = new SpellString(spellDate, mapValuesString.get(spellDate));
                listSpellsString.add(spellString);
            }
        }



        StringBuilder oneEdgeAttValues = new StringBuilder();
        StringBuilder oneEdgeSpells = new StringBuilder();
        StringBuilder oneEdgeFull = new StringBuilder();



        // NOW we iterate through the spells for FLOAT attributes
        // to create the "attvalue = ..." in the gexf file (finally!)

        oneEdgeAttValues.append("    <attvalues>\n");

        Iterator<SpellFloat> listSpellsItFloat = listSpellsFloat.iterator();

        while (listSpellsItFloat.hasNext()) {

            SpellFloat currSpell = listSpellsItFloat.next();

            Iterator<Map.Entry<String, Float>> floatValuesOfCurrentSpellIterator = currSpell.floatValues.entrySet().iterator();
            while (floatValuesOfCurrentSpellIterator.hasNext()) {

                Map.Entry<String, Float> currEntry = floatValuesOfCurrentSpellIterator.next();
                String currEntryName = currEntry.getKey();
                Float currEntryValue = currEntry.getValue();

                if (Main.edgeWeight.equals(currEntryName)) {
                    oneEdgeAttValues.append("        <attvalue for=\"weight\" value=\"").append(currEntryValue).append("\" start=\"");
                } else {
                    oneEdgeAttValues.append("        <attvalue for=\"").append(currEntryName).append("\" value=\"").append(currEntryValue).append("\" start=\"");
                }

                oneEdgeAttValues.append(currSpell.date).append("\" ");
                oneEdgeAttValues.append("end=\"");
                oneEdgeAttValues.append(currSpell.date).append("\" ");
                oneEdgeAttValues.append("/>");
                oneEdgeAttValues.append("\n");
            }

        }

        // NOW we iterate through the spells for STRING attributes
        // to create the "attvalue = ..." in the gexf file (finally!)
        Iterator<SpellString> listSpellsItString = listSpellsString.iterator();

        while (listSpellsItString.hasNext()) {

            SpellString currSpell = listSpellsItString.next();

            Iterator<Map.Entry<String, String>> stringValuesOfCurrentSpellIterator = currSpell.stringValues.entrySet().iterator();
            while (stringValuesOfCurrentSpellIterator.hasNext()) {

                Map.Entry<String, String> currEntry = stringValuesOfCurrentSpellIterator.next();
                String currEntryName = currEntry.getKey();
                String currEntryValue = currEntry.getValue();

                if (Main.edgeWeight.equals(currEntryName)) {
                    oneEdgeAttValues.append("        <attvalue for=\"weight\" value=\"").append(currEntryValue).append("\" start=\"");
                } else {
                    oneEdgeAttValues.append("        <attvalue for=\"").append(currEntryName).append("\" value=\"").append(currEntryValue).append("\" start=\"");
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
        setStartEndDates = ConsecutiveSpellsCleaner.doCleaning(setMultiDates);
        
        
        //!!!! iterates through all the dates for this edge and creates corresponding spells
        Iterator<PairDates> iteratorStartEndDates = setStartEndDates.iterator();
        while (iteratorStartEndDates.hasNext()) {
            PairDates<LocalDate, LocalDate> currPair = iteratorStartEndDates.next();
            LocalDate startDate = currPair.getLeft();
            LocalDate endDate = currPair.getRight();
            oneEdgeSpells.append("        <spell start=\"");
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
            oneEdgeFull.append("    </attvalues>\n");
        }
        oneEdgeFull.append("    <spells>\n");
        oneEdgeFull.append(oneEdgeSpells);
        oneEdgeFull.append("    </spells>\n");
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

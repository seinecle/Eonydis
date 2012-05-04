/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eonydis;

import com.google.common.collect.TreeMultiset;
import java.util.*;
import java.util.Map.Entry;
import levallois.clement.utils.PairDates;
import levallois.clement.utils.Triple;
import org.joda.time.LocalDate;

/**
 *
 * @author Clement
 */
public class WorkerThreadNodes implements Runnable {

    private final String currNode;
    private float currValueFloat;
    private String currValueString;
    private HashSet<String> setNodeAttributes = new HashSet();
    private boolean onlyOneRecordNeedsToBeRead = true;

    WorkerThreadNodes(String nodeId) {

        this.currNode = nodeId;
        run();


    }

    @Override
    public void run() {


        LinkedList<SpellFloat> listSpellsFloat = new LinkedList();
        LinkedList<SpellString> listSpellsString = new LinkedList();

        HashMap mapNodeStaticAttributesToTheirValues = new HashMap();

        //a treeset to keep a the dates in chronological order
        TreeMultiset<PairDates> setStartEndDates;

        TreeMultiset<LocalDate> setMultiDates = TreeMultiset.create();
        //this will record the values corresponding to the multiple dates
        // since we have multiple attributes, WITHIN EACH TRANSACTION we will have to loop for as many edge attributes as the user has defined in Main.edgeAttributes
        HashMap<LocalDate, HashMap<String, Float>> mapValuesFloat = new HashMap();
        HashMap<LocalDate, HashMap<String, String>> mapValuesString = new HashMap();

        boolean oneStringAttributeDetected = false;


        //iterate through the list of all pairs (one Node, one date)
        Iterator<Triple<Node, LocalDate, HashMap<String, String>>> listNodesAndDatesIterator = Main.listNodesAndDates.iterator();
        while (listNodesAndDatesIterator.hasNext()) {

            Triple<Node, LocalDate, HashMap<String, String>> currNodeAndDate = listNodesAndDatesIterator.next();
            String currNodeId = currNodeAndDate.getLeft().getNodeId();
            LocalDate currDate = currNodeAndDate.getMiddle();
            HashMap<String, String> currAllFieldValues = currNodeAndDate.getRight();


            HashMap<String, String> stringValues = new HashMap();

            //when the Node of interest is found in the list, add the corresponding date to a set of dates for this Node
            if (currNodeId.equals(currNode)) {




                setNodeAttributes.clear();

                if (currNodeId.equals(currAllFieldValues.get(Main.source))) {

//                    nodeAttributes = new String[Main.sourceAttributes.length];
                    setNodeAttributes.addAll(Arrays.asList(Main.sourceAttributes));
//                    System.arraycopy(Main.sourceAttributes, 0, nodeAttributes, 0, Main.sourceAttributes.length);

                } else {

//                    nodeAttributes = new String[Main.targetAttributes.length];
                    setNodeAttributes.addAll(Arrays.asList(Main.targetAttributes));
//                    System.arraycopy(Main.targetAttributes, 0, nodeAttributes, 0, Main.targetAttributes.length);

                }


                //THIS TAKES CARE OF STATIC NODE ATTRIBUTES
                if (onlyOneRecordNeedsToBeRead) {
                    Iterator<String> setNodeStaticAttributesIterator = Main.setNodeStaticAttributes.iterator();
                    while (setNodeStaticAttributesIterator.hasNext()) {
                        String currNodeStaticAttribute = setNodeStaticAttributesIterator.next();
                        if (setNodeAttributes.contains(currNodeStaticAttribute)) {
                            mapNodeStaticAttributesToTheirValues.put(currNodeStaticAttribute, currAllFieldValues.get(currNodeStaticAttribute));
                        }

                    }
                    onlyOneRecordNeedsToBeRead = false;
                }

                //DELETION OF THE ATTRIBUTES WHICH ARE STATIC TO OBTAIN A LIST OF ONLY DYNAMIC ATTRIBUTES
                Iterator<String> setNodeStaticAttributesIterator = Main.setNodeStaticAttributes.iterator();
                while (setNodeStaticAttributesIterator.hasNext()) {
                    setNodeAttributes.remove(setNodeStaticAttributesIterator.next());
                }



                //beginning of the loop through node dynamic attributes selected by the user. Each attribute value will be stored
                
                Iterator<String> setNodeAttributesIterator = setNodeAttributes.iterator();

                while (setNodeAttributesIterator.hasNext()) {
                    String currNodeAttribute = setNodeAttributesIterator.next();


                    //beginning of the loop through edge attributes
//                    System.out.println("in node worker - current node attribute is "+Main.nodeAttributes[k]+", corresponding to indice "+k);

                    if (Main.stringAttributes.contains(currNodeAttribute)) {

                        try {
                            currValueString = currAllFieldValues.get(currNodeAttribute);
                            oneStringAttributeDetected = true;
                        } catch (NullPointerException e) {
                            currValueString = "";
                        }
                        stringValues.put(currNodeAttribute, currValueString);

                        continue;


                    }

                    //end of the case STRING values. Beginning case FLOAT values
                    //This try-catch treats null values for attributes as zeros.
                    try {
                        currValueFloat = Float.parseFloat(currAllFieldValues.get(currNodeAttribute));
                    } catch (NumberFormatException e) {
                        currValueFloat = (float) 0;
                    }

                    HashMap<String, Float> storedFloatValues = new HashMap();
                    //if the list of (dates, values) already contains the date of the current transaction
                    if (mapValuesFloat.containsKey(currDate)) {

                        //retrieve the current value stored for the date
                        storedFloatValues = mapValuesFloat.get(currDate);

                        //add the currValue to the storedValue
                        //System.out.println("storedValue[j] " + storedValue[j]);
                        //System.out.println("currValue " + currValue);

                        //These two lines treat null values for attributes as zeros.
                        if (storedFloatValues.get(currNodeAttribute) == null) {
                            storedFloatValues.put(currNodeAttribute, (float) 0);
                        }

                        //add the currValue to the storedValue
                        storedFloatValues.put(currNodeAttribute, storedFloatValues.get(currNodeAttribute) + currValueFloat);

                        //reinput the new storedValue in the list of (dates,Values)
                        mapValuesFloat.put(currDate, storedFloatValues);

                    } else {
                        storedFloatValues.put(currNodeAttribute, currValueFloat);
                        mapValuesFloat.put(currDate, storedFloatValues);

                    }

                }



                if (oneStringAttributeDetected) {

                    mapValuesString.put(currDate, stringValues);
                }

                //add the date to an ordered multiset
                setMultiDates.add(currDate);
            }


        }// end of loop through the list of all pairs <Node,Date>

//        System.out.println("dates of the node: " + setMultiDates.entrySet().toString());
        // ######################################################################

        // What we have now is:
        // HashMap<LocalDate, HashMap<String, Float>> mapValuesFloat = new HashMap();
        // HashMap<LocalDate, HashMap<String, String>> mapValuesString = new HashMap();

        // These two maps contain the set of Dates where the node appear, and for each date, the map of the attributes selected by the user.

        // We iterate through each of them to build spell. Honestly, not sure this is necessary, except that it helps deal with the averaging / summing case for Floats

        //Iterate through all the unique dates and their unique values for FLOAT
        Iterator<LocalDate> itSpells = mapValuesFloat.keySet().iterator();
        while (itSpells.hasNext()) {
            LocalDate spellDate = itSpells.next();

            // this applies the averaging of values appearing on the same date, or summing depending on the user's selection

            HashMap<String, Float> tempFloatValues = new HashMap();

            Iterator<Entry<String, Float>> mapValuesFloatCurrDateIterator = mapValuesFloat.get(spellDate).entrySet().iterator();
            while (mapValuesFloatCurrDateIterator.hasNext()) {

                Entry<String, Float> currEntry = mapValuesFloatCurrDateIterator.next();

                if (Main.averageAttributes.contains(currEntry.getKey())) {
                    tempFloatValues.put(currEntry.getKey(), currEntry.getValue() / (float) setMultiDates.count(spellDate));
                } else {
                    tempFloatValues.put(currEntry.getKey(), currEntry.getValue());
                }
            }

            mapValuesFloat.put(spellDate, tempFloatValues);


            SpellFloat spell = new SpellFloat(spellDate, mapValuesFloat.get(spellDate));
            listSpellsFloat.add(spell);
        }

        if (oneStringAttributeDetected) {
            itSpells = mapValuesString.keySet().iterator();
            while (itSpells.hasNext()) {
                LocalDate spellDate = itSpells.next();
                SpellString spellString = new SpellString(spellDate, mapValuesString.get(spellDate));
                listSpellsString.add(spellString);
            }
        }



        StringBuilder oneNodeAttValues = new StringBuilder();
        StringBuilder oneNodeSpells = new StringBuilder();
        StringBuilder oneNodeFull = new StringBuilder();



        // DEALS WITH STATIC VALUES
        Iterator<Entry<String, String>> mapNodeStaticAttributesToTheirValuesIterator = mapNodeStaticAttributesToTheirValues.entrySet().iterator();
        while (mapNodeStaticAttributesToTheirValuesIterator.hasNext()) {

            Entry<String, String> currEntry = mapNodeStaticAttributesToTheirValuesIterator.next();
            oneNodeAttValues.append("        <attvalue for=\"").append(currEntry.getKey()).append("\" value=\"").append(currEntry.getValue()).append("\"/>\n");
        }

        //DEALS WTH DYNAMIC VALUES - FLOAT
        Iterator<SpellFloat> listSpellsItFloat = listSpellsFloat.iterator();
        while (listSpellsItFloat.hasNext()) {

            SpellFloat currSpell = listSpellsItFloat.next();

            Iterator<Entry<String, Float>> floatValuesOfCurrentSpellIterator = currSpell.floatValues.entrySet().iterator();
            while (floatValuesOfCurrentSpellIterator.hasNext()) {

                Entry<String, Float> currEntry = floatValuesOfCurrentSpellIterator.next();
                String currEntryName = currEntry.getKey();
                Float currEntryValue = currEntry.getValue();

                oneNodeAttValues.append("        <attvalue for=\"").append(currEntryName).append("\" value=\"").append(currEntryValue).append("\" start=\"");


                oneNodeAttValues.append(currSpell.date).append("\" ");
                oneNodeAttValues.append("end=\"");
                oneNodeAttValues.append(currSpell.date).append("\" ");
                oneNodeAttValues.append("/>");
                oneNodeAttValues.append("\n");
            }
        }


        //DEALS WTH DYNAMIC VALUES - STRING
        Iterator<SpellString> listSpellsItString = listSpellsString.iterator();

        while (listSpellsItString.hasNext()) {

            SpellString currSpell = listSpellsItString.next();

            Iterator<Entry<String, String>> stringValuesOfCurrentSpellIterator = currSpell.stringValues.entrySet().iterator();
            while (stringValuesOfCurrentSpellIterator.hasNext()) {

                Entry<String, String> currEntry = stringValuesOfCurrentSpellIterator.next();
                String currEntryName = currEntry.getKey();
                String currEntryValue = currEntry.getValue();

                oneNodeAttValues.append("        <attvalue for=\"").append(currEntryName).append("\" value=\"").append(currEntryValue).append("\" start=\"");


                oneNodeAttValues.append(currSpell.date).append("\" ");
                oneNodeAttValues.append("end=\"");
                oneNodeAttValues.append(currSpell.date).append("\" ");
                oneNodeAttValues.append("/>");
                oneNodeAttValues.append("\n");
            }
        }



        // We also have a setMultiDates of all the dates when the two banks interact
        // This setMultiDates needs to be modified to delete the dates that are consecutive

        setStartEndDates = ConsecutiveSpellsCleaner.doCleaning(setMultiDates);



        // FINALLY, iterate through all the dates for this node and creates corresponding spells

        Iterator<PairDates> iteratorStartEndDates = setStartEndDates.iterator();
        while (iteratorStartEndDates.hasNext()) {
            PairDates<LocalDate, LocalDate> currPair = iteratorStartEndDates.next();
            LocalDate startDate = currPair.getLeft();
            LocalDate endDate = currPair.getRight();
            System.out.println("printing the next date:" + startDate);

            oneNodeSpells.append("        <spell start=\"");
            oneNodeSpells.append(startDate).append("\" ");
            oneNodeSpells.append("end=\"");
            oneNodeSpells.append(endDate).append("\" ");
            oneNodeSpells.append("/>");
            oneNodeSpells.append("\n");
            System.out.println("oneNodeSpells = " + oneNodeSpells.toString());
        }//end of the iteration trough dates





        oneNodeFull.append("<node id=\"");
        oneNodeFull.append(currNode);
        oneNodeFull.append("\" ");
        oneNodeFull.append("label=\"");
        oneNodeFull.append(currNode);
        oneNodeFull.append("\">");
        oneNodeFull.append("\n");
        oneNodeFull.append("    <attvalues>");
        oneNodeFull.append("\n");
        oneNodeFull.append(oneNodeAttValues);
        oneNodeFull.append("    </attvalues>\n");
        oneNodeFull.append("    <spells>\n");
        oneNodeFull.append(oneNodeSpells);
        System.out.println("oneNodeSpells at this stage is: " + oneNodeSpells.toString());
        oneNodeFull.append("    </spells>\n");
        oneNodeFull.append("</node>\n");

        Main.nodes.append(oneNodeFull);

    } //end looping throught the set of unique Nodes
}

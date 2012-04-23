/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eonydis;

import com.google.common.collect.TreeMultiset;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;
import levallois.clement.utils.PairDates;
import org.joda.time.LocalDate;

/**
 *
 * @author Clement
 */
public class WorkerThreadNodes implements Runnable {

    private final String currNode;
    private int counterDates;
    private boolean newSpell2;
    private float currValue;
    private Float[] emptyValues = new Float[0];
    private String [] nodeAttributes;

    WorkerThreadNodes(String nodeId) {

        this.currNode = nodeId;
        run();


    }

    @Override
    public void run() {


        LinkedList<Spell> listSpells = new LinkedList();
        //a treeset to keep a the dates in chronological order

        TreeMultiset<PairDates> setStartEndDates = TreeMultiset.create();

        TreeMultiset<LocalDate> setMultiDates = TreeMultiset.create();
        TreeMap<LocalDate, Float[]> setMultiValues = new TreeMap();


        //iterate through the list of all pairs (one Node, one date)
        for (int i = 0; i < Main.listNodesAndDates.size(); i++) {

            String currNodeId = Main.listNodesAndDates.get(i).getLeft().getNodeId();



            //when the Node of interest is found in the list, add the corresponding date to a set of dates for this Node
            if (currNodeId.equals(currNode)) {

                
                if (currNode.equals(Main.listNodesAndDates.get(i).getRight().get(Main.source))){
                    
                    nodeAttributes = new String [Main.sourceAttributes.length];
                    System.arraycopy(Main.sourceAttributes, 0, nodeAttributes, 0, Main.sourceAttributes.length);
                    
                }
                
                else {
                    
                    nodeAttributes = new String [Main.targetAttributes.length];
                    System.arraycopy(Main.targetAttributes, 0, nodeAttributes, 0, Main.targetAttributes.length);

                
                }
                
                //beginning of the loop through node attributes selected by the user. Each attribute value will be stored
                //if multiple attributes exist for the same date, they get AVERAGED (summing would be an easy implementation too)

                for (int j = 0; j < nodeAttributes.length; j++) {

                    //This try-catch treats null values for attributes as zeros.
                    try {
                        currValue = Float.parseFloat(Main.listNodesAndDates.get(i).getRight().get(nodeAttributes[j]));
                    } catch (NumberFormatException e) {
                        currValue = (float) 0;
                    }





                    //if the list of (dates, values) already contains the date of the current transaction
                    if (setMultiValues.containsKey(Main.listNodesAndDates.get(i).getMiddle())) {

                        //retrieve the current value stored for the date
                        Float[] storedValue = setMultiValues.get(Main.listNodesAndDates.get(i).getMiddle());

                        //add the currValue to the storedValue
                        //System.out.println("storedValue[j] " + storedValue[j]);
                        //System.out.println("currValue " + currValue);

                        //These two lines treat null values for attributes as zeros.
                        if (storedValue[j] == null) {
                            storedValue[j] = (float) (0);
                        }

                        storedValue[j] = storedValue[j] + currValue;

                        //reinput the new storedValue in the list of (dates,Values)
                        setMultiValues.put(Main.listNodesAndDates.get(i).getMiddle(), storedValue);
                    
                        
                    // if this is a new date, create a new date and the corresponding new value    
                    } else {
                        Float[] attValues = new Float[nodeAttributes.length];
                        attValues[j] = currValue;
                        setMultiValues.put(Main.listNodesAndDates.get(i).getMiddle(), attValues);

                    }

                }

                //deals with the case when no node attributes have been selected.
                if (nodeAttributes.length == 0) {
                    setMultiValues.put(Main.listNodesAndDates.get(i).getMiddle(), emptyValues);

                }                
                

                //add the date to an ordered multiset
                setMultiDates.add(Main.listNodesAndDates.get(i).getMiddle());
            }

        }// end of loop through the list of all pairs <Node,Date>

        //iterate through all the unique dates and their unique values
        Iterator<LocalDate> itSpells = setMultiValues.keySet().iterator();
        while (itSpells.hasNext()) {
            LocalDate spellDate = itSpells.next();

            //a spell is actually the spell itself + the associated value for the attributes of the node at this date
            // the associated value
            for (int j = 0; j < nodeAttributes.length; j++) {

                //that's the line which implements the AVERAGE. Summing would simply drop the divisor
                setMultiValues.get(spellDate)[j] = (float)setMultiValues.get(spellDate)[j] / (float)(setMultiDates.count(spellDate));
               
            }
            
            //deals with the case when no node attributes were selected by the user
            if (nodeAttributes.length == 0) {
                Spell spell = new Spell(spellDate, emptyValues);
                listSpells.add(spell);
            }
            
            Spell spell = new Spell(spellDate, setMultiValues.get(spellDate));
            listSpells.add(spell);
        }

        StringBuilder oneNodeAttValues = new StringBuilder();
        StringBuilder oneNodeSpells = new StringBuilder();
        StringBuilder oneNodeFull = new StringBuilder();


        Iterator<Spell> listSpellsIt = listSpells.iterator();


        while (listSpellsIt.hasNext()) {

            Spell currSpell = listSpellsIt.next();
            for (int i = 0; i < currSpell.values.length; i++) {
                oneNodeAttValues.append("        <attvalue for=\"")
                        .append(nodeAttributes[i]).append("\" value=\"")
                        .append(currSpell.values[i]).append("\" start=\"");


                oneNodeAttValues.append(currSpell.date).append("\" ");
                oneNodeAttValues.append("end=\"");
                oneNodeAttValues.append(currSpell.date).append("\" ");
                oneNodeAttValues.append("/>");
                oneNodeAttValues.append("\n");
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
            oneNodeSpells.append("        <spell start=\"");
            oneNodeSpells.append(startDate).append("\" ");
            oneNodeSpells.append("end=\"");
            oneNodeSpells.append(endDate).append("\" ");
            oneNodeSpells.append("/>");
            oneNodeSpells.append("\n");

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
        oneNodeFull.append("    </spells>\n");
        oneNodeFull.append("</node>\n");

        Main.nodes.append(oneNodeFull);






    } //end looping throught the set of unique Nodes
}

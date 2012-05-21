/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eonydis;

import java.util.ArrayList;
import java.util.HashMap;
import levallois.clement.utils.Pair;
import levallois.clement.utils.Triple;
import org.joda.time.LocalDate;

/**
 *
 * @author dnb
 */
public class Transaction {

    private HashMap<String, String> mapTransFull = new HashMap();
    static public ArrayList<Transaction> listTransactions = new ArrayList();
    boolean nodekAddedLn = false;
    boolean nodeAddedBr = false;
    boolean nodeAddedAll = false;
    int year;
    int month;
    int day;
    int hour;
    int minute;
    String [] arrayTime;
    ArrayList <Integer> arrayListTime = new ArrayList();
    private final LocalDate dt;


    //Beginning Constructor
    Transaction(String[] values) {
        for (int i = 0; i < Main.headers.length; i++) {
            
            mapTransFull.put(Main.headers[i], values[i]);

            //Bank bank = new Node();


            }

            //adds the 2 banks of this current transaction
            // to the set all the banks are present in the dataset - simply referred by their BIC
            nodeAddedAll = Main.setNodes.add(mapTransFull.get(Main.source));
            nodeAddedAll = Main.setNodes.add(mapTransFull.get(Main.target));
            
            //add the pair of Nodes to a set of unique pairs of Nodes
            Main.setPairsNodes.add(mapTransFull.get(Main.source).concat(mapTransFull.get(Main.target)));
            

            // parse the field "ln_date" to format the time in a proper way
//            arrayTime = mapTransFull.get("ln_time").split("/");
//            month = Integer.valueOf(arrayTime[0]);
//            System.out.println("value of Month with the usual method: "+month);
//            day = Integer.valueOf(arrayTime[1]);
//            year = Integer.valueOf(arrayTime[2].split(" ")[0]);

            String timeField = mapTransFull.get(Main.timeField);
//            System.out.println("timeField"+timeField);
            month = Integer.valueOf(timeField.replaceAll(Main.userDefinedTimeFormat, "$"+Main.mapOrderTimeFields.get("month")));
            day = Integer.valueOf(timeField.replaceAll(Main.userDefinedTimeFormat, "$"+Main.mapOrderTimeFields.get("day")));
            year = Integer.valueOf(timeField.replaceAll(Main.userDefinedTimeFormat, "$"+Main.mapOrderTimeFields.get("year")));
            
            
            //stores the time in a dt object (using the Joda library)
            dt = new LocalDate(year,month,day);
            //creates an object "nodeSource" to store in a neat way the details of the lending bank of this transaction (see "Node" class to see the details)
            Node nodeSource = new Node(mapTransFull.get(Main.source));
            
            //creates an object "nodeTarget" to store in a neat way the details of the borrowing bank of this transaction (see "Node" class to see the details)
            Node nodeTarget = new Node(mapTransFull.get(Main.target));

            Main.listTransactionsAndDates.add(new Triple(mapTransFull,new Pair(nodeSource.getNodeId(),nodeTarget.getNodeId()),dt));           
            
            //adds pairs of the objects created above to a list storing these pairs
            Main.listNodesAndDates.add(new Triple(nodeSource,dt,mapTransFull));
            Main.listNodesAndDates.add(new Triple(nodeTarget,dt,mapTransFull));
            //System.out.println(Node.getlistBanksAndDates().size());


    }//End Constructor


    
    public HashMap<String, String> getMapTransFull() {

        return mapTransFull;
    }


    public String getLnValue() {

        return mapTransFull.get("ln_value");
    }

    public String getRate() {

        return mapTransFull.get("rate");
    }





} // End class Transaction


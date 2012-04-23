/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eonydis;

import com.csvreader.CsvReader;
import gui.GUIMain;
import gui.Screen2;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import levallois.clement.utils.Clock;
import levallois.clement.utils.Pair;
import levallois.clement.utils.Triple;
import org.joda.time.LocalDate;

/**
 *
 * @author dnb
 */
public class Main implements Runnable {

    static public String[] headers;
    static public boolean readHeaders;
    static CsvReader transactionsCsv;
    static int j = 0;
    // Triple consists of: a full transaction, the pair of nodes involved in this transaction, the date of the transaction
    public static ArrayList<Triple<HashMap<String, String>, Pair<String, String>, LocalDate>> listTransactionsAndDates = new ArrayList();
    public static HashSet<String> setNodes = new HashSet();
    public static HashSet<String> setNodesLn = new HashSet();
    public static HashSet<String> setNodesBr = new HashSet();
    public static HashSet<String> setPairsNodes = new HashSet();
    public static ArrayList<Triple<Node, LocalDate, HashMap<String, String>>> listNodesAndDates = new ArrayList();
    public static int nodeCounter;
//    static int numberOfThreads = Runtime.getRuntime().availableProcessors();
//    static int numberOfThreads = 1;
//    static ExecutorService pool = Executors.newFixedThreadPool(numberOfThreads);
    public static ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue();
    public static StringBuilder nodes = new StringBuilder();
    public static StringBuilder edges = new StringBuilder();
    public static String fileName = GUIMain.screen1.fileSelectedName.substring(0, GUIMain.screen1.fileSelectedName.lastIndexOf("."));
    public static String javaGEXFoutput;
    public static String[] arrayValues;
    public static Screen2 screen2;
    public static String[] nodeAttributes;
    public static String[] edgeAttributes;
    public static Object[] edgeAttributesObjects;
    public static String edgeWeight;
    public static String[] sourceAttributes;
    public static Object[] sourceAttributesObjects;
    public static String[] targetAttributes;
    public static Object[] targetAttributesObjects;
    public static String userDefinedTimeFormat;
    public static String timeField;
    String pathFile;
    public String nameFile;
    public static String source;
    public static String target;
    public static String currLine;
    public static TreeMap<Integer, String> mapPOSinTimePattern = new TreeMap();
    public static HashMap<String, Integer> mapOrderTimeFields = new HashMap();
    public static String[] orderOfTimeFieldsInUserPattern;
    static int countNodesLoops = 0;
    public static boolean doAverage;
    public static int lengthEdgeAttributesArray;

    public Main(String wk, String file, String doAverage) {

        pathFile = wk + "\\";
        nameFile = file;
        this.doAverage = Boolean.getBoolean(doAverage);

    }

    @Override
    public void run() {
        try {



            javaGEXFoutput = pathFile + fileName + "_eonydis.gexf";
            System.out.println("file name is: " + javaGEXFoutput);

            transactionsCsv = new CsvReader(pathFile + nameFile);
            readHeaders = transactionsCsv.readHeaders();
            headers = transactionsCsv.getHeaders();

            GUIMain.screen1.setVisible(false);
            screen2 = new Screen2();
            screen2.setVisible(true);


        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }




    }

    public static void selectSource() {
        source = (String) Screen2.listFields.getSelectedValue();
        System.out.println("source is: " + source);
        Screen2.model.removeElement(source);
        Screen2.toBeSelected.setText("<html>select <b>target</b> node</html>");
        Screen2.count = 2;
    }

    public static void selectTarget() {
        target = (String) Screen2.listFields.getSelectedValue();
        System.out.println("target is: " + target);
        Screen2.model.removeElement(target);
        Screen2.toBeSelected.setText("<html>select attribute(s) for <b>source nodes</b><br>(use shift and ctrl to select multiple attributes)<br>(no attributes? just click next)</html>");
        Screen2.count = 3;
    }

    public static void selectSourceAttributes() {
        sourceAttributesObjects = Screen2.listFields.getSelectedValues();
        sourceAttributes = new String[sourceAttributesObjects.length];

        for (int i = 0; i < sourceAttributesObjects.length; i++) {
            System.out.println("selected source attribute(s): " + sourceAttributesObjects[i].toString());
            Screen2.model.removeElement(sourceAttributesObjects[i]);
            sourceAttributes[i] = sourceAttributesObjects[i].toString();
        }

        Screen2.toBeSelected.setText("<html>select attribute(s) for <b>target nodes</b><br>(use shift and ctrl to select multiple attributes)<br>(no attributes? just click next)</html>");
        Screen2.count = 4;
    }

    public static void selectTargetAttributes() {
        targetAttributesObjects = Screen2.listFields.getSelectedValues();
        targetAttributes = new String[targetAttributesObjects.length];

        for (int i = 0; i < targetAttributesObjects.length; i++) {
            System.out.println("selected target attribute(s): " + targetAttributesObjects[i].toString());
            Screen2.model.removeElement(targetAttributesObjects[i]);
            targetAttributes[i] = targetAttributesObjects[i].toString();
        }

        Screen2.toBeSelected.setText("<html>select one attribute for <b>edge weight</b><br>(no attribute for weight? just click next)</html>");
        Screen2.count = 5;
    }

    public static void selectEdgeWeight() {
        edgeWeight = (String) Screen2.listFields.getSelectedValue();
        System.out.println("edge weight is: " + edgeWeight);

        Screen2.model.removeElement(edgeWeight);

        Screen2.toBeSelected.setText("<html>select other <b>edge attribute(s)</b><br>(use shift and ctrl to select multiple attributes)<br>(no attributes? just click next)</html>");
        Screen2.count = 6;

    }

    public static void selectEdgeAttributes() {
        edgeAttributesObjects = Screen2.listFields.getSelectedValues();
        edgeAttributes = new String[edgeAttributesObjects.length];

        for (int i = 0; i < edgeAttributesObjects.length; i++) {
            System.out.println("selected edge attribute(s): " + edgeAttributesObjects[i].toString());
            Screen2.model.removeElement(edgeAttributesObjects[i]);
            edgeAttributes[i] = edgeAttributesObjects[i].toString();

        }
        Screen2.toBeSelected.setText("<html>select <b>time field<b></html>");
        Screen2.count = 7;


        //this appendix to SelectEdgeAttributes adds the edge attribute selected for weight - if any

        if (edgeWeight != null) {
            String[] tempEdgeAttributes = new String[edgeAttributes.length + 1];
            System.arraycopy(edgeAttributes, 0, tempEdgeAttributes, 0, edgeAttributes.length);
            tempEdgeAttributes[tempEdgeAttributes.length - 1] = edgeWeight;
            edgeAttributes = new String[tempEdgeAttributes.length];
            System.arraycopy(tempEdgeAttributes, 0, edgeAttributes, 0, edgeAttributes.length);
        }



    }

    public static void selectTimeField() {
        timeField = (String) Screen2.listFields.getSelectedValue();
        System.out.println("time field is: " + timeField);

        Screen2.model.removeElement(timeField);
        Main.screen2.setVisible(false);
        GUIMain.screen4.setVisible(true);

        Screen2.OKButton.setText("<html><b>create gexf</b></html>");
        Screen2.count = 8;

    }

    //reads lines of the csv file and instanciates as many objects from the Transaction class
    public static void populateTransactions() throws IOException {
        Clock readTransactions = new Clock("reading each line of the csv file");

        int indexStartYear = userDefinedTimeFormat.indexOf("y");
        System.out.println("indexStartYear is: " + indexStartYear);

        int indexStartMonth = userDefinedTimeFormat.indexOf("m");
        System.out.println("indexStartMonth is: " + indexStartMonth);

        int indexStartDay = userDefinedTimeFormat.indexOf("d");
        System.out.println("indexStartDay is: " + indexStartDay);

        int indexStartHour = userDefinedTimeFormat.indexOf("h");
        System.out.println("indexStartHour is: " + indexStartHour);

        int indexStartMinute = userDefinedTimeFormat.indexOf("i");
        System.out.println("indexStartMinute is: " + indexStartMinute);

        int indexStartSecond = userDefinedTimeFormat.indexOf("s");
        System.out.println("indexStartSecond is: " + indexStartSecond);

        userDefinedTimeFormat = userDefinedTimeFormat.replace("yyyy", "(\\d*)");
        userDefinedTimeFormat = userDefinedTimeFormat.replace("#", ".");
        userDefinedTimeFormat = userDefinedTimeFormat.replace("mm", "(\\d*)");
        userDefinedTimeFormat = userDefinedTimeFormat.replace("dd", "(\\d*)");
        userDefinedTimeFormat = userDefinedTimeFormat.replace("hh", "(\\d*)");
        userDefinedTimeFormat = userDefinedTimeFormat.replace("ii", "(\\d*)");
        userDefinedTimeFormat = userDefinedTimeFormat.replace("ss", "(\\d*)");
        System.out.println("regex pattern is: " + userDefinedTimeFormat);


        if (indexStartYear != -1) {
            mapPOSinTimePattern.put(indexStartYear, "year");
        }
        if (indexStartMonth != -1) {
            mapPOSinTimePattern.put(indexStartMonth, "month");
        }
        if (indexStartDay != -1) {
            mapPOSinTimePattern.put(indexStartDay, "day");
        }
        if (indexStartHour != -1) {
            mapPOSinTimePattern.put(indexStartHour, "hour");
        }
        if (indexStartMinute != -1) {
            mapPOSinTimePattern.put(indexStartMinute, "minute");
        }
        if (indexStartSecond != -1) {
            mapPOSinTimePattern.put(indexStartSecond, "second");
        }

        Iterator<Integer> itTimeFields = mapPOSinTimePattern.keySet().iterator();
        int j = 0;
        orderOfTimeFieldsInUserPattern = new String[mapPOSinTimePattern.keySet().size()];

        while (itTimeFields.hasNext()) {
            int currRank = itTimeFields.next();

            orderOfTimeFieldsInUserPattern[j] = mapPOSinTimePattern.get(currRank);
            System.out.println("group number for " + orderOfTimeFieldsInUserPattern[j] + " is " + (j + 1));
            j++;
        }

        for (int i = 0; i < orderOfTimeFieldsInUserPattern.length; i++) {
            mapOrderTimeFields.put(orderOfTimeFieldsInUserPattern[i], i + 1);

        }



        while (transactionsCsv.readRecord()) {
            new Transaction(transactionsCsv.getValues());
        }
        readTransactions.closeAndPrintClock();
    }

    //loops through the list of nodes and creates the gexf from it
    public static void createGexfIntro() throws IOException {

        // writing the header of the gexf file
        // IMPORTANT: declaration of attributes should be modified manually here!!    

        Clock headerWriting = new Clock("writing the header of the gexf file");


        //this creates an array of node attributes made of the attributes of source nodes AND attributes of the target nodes
        nodeAttributes = new String[sourceAttributes.length + targetAttributes.length];

        for (int i = 0; i < sourceAttributes.length; i++) {
            nodeAttributes[i] = sourceAttributes[i];
        }
        for (int i = 0; i < targetAttributes.length; i++) {
            nodeAttributes[i + sourceAttributes.length] = targetAttributes[i];
        }


        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        BufferedWriter bw = new BufferedWriter(new FileWriter(javaGEXFoutput));
        bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        bw.newLine();
        bw.write("<gexf xmlns=\"http://www.gexf.net/1.2draft\" version=\"1.2\" xmlns:viz=\"http://www.gexf.net/1.2draft/viz\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.gexf.net/1.2draft http://www.gexf.net/1.2draft/gexf.xsd\">");
        bw.newLine();
        bw.write("    <meta lastmodifieddate=\"" + dateFormat.format(date) + "\">");
        bw.newLine();
        bw.write("<creator>gexf file generated with Eonydis - see www.clementlevallois.net</creator>");
        bw.newLine();
        bw.write("    </meta>");
        bw.newLine();
        bw.write("    <graph defaultedgetype=\"directed\" timeformat=\"date\" mode=\"dynamic\">");
        bw.newLine();
        bw.write("    <attributes class=\"node\" mode=\"dynamic\">");
        bw.newLine();

        for (int i = 0; i < nodeAttributes.length; i++) {
            bw.write("      <attribute id=\"" + nodeAttributes[i] + "\" title=\"" + nodeAttributes[i] + "\" type=\"float\"></attribute>");
            bw.newLine();
        }
        bw.write("    </attributes>");
        bw.newLine();
        bw.write("    <attributes class=\"edge\" mode=\"dynamic\">");
        bw.newLine();

        if (edgeWeight != null) {
            bw.write("      <attribute id=\"weight\" title=\"Weight\" type=\"float\"></attribute>");
            bw.newLine();
            lengthEdgeAttributesArray = edgeAttributes.length - 1;

        } else {
            lengthEdgeAttributesArray = edgeAttributes.length;
        }

        for (int i = 0; i < lengthEdgeAttributesArray; i++) {
            bw.write("      <attribute id=\"" + edgeAttributes[i] + "\" title=\"" + edgeAttributes[i] + "\" type=\"float\"></attribute>");
            bw.newLine();
        }

        bw.write("    </attributes>");
        bw.newLine();
        bw.write("<nodes>");
        bw.newLine();
        bw.close();

        headerWriting.closeAndPrintClock();
    }

    public static void createGexfNodes() throws IOException, InterruptedException {

        Clock writeGexfNodes = new Clock("writing the nodes into the gexf file");

        //iterate through the set of all nodes
        Iterator it = setNodes.iterator();

        System.out.println("Number of nodes: " + setNodes.size());
        while (it.hasNext()) {
            countNodesLoops++;
            GUIMain.screen6.jProgressBar1.setValue(Math.round((float) countNodesLoops / (float) (setNodes.size() + listTransactionsAndDates.size()) * 100));

            String currNode = (String) it.next();

            new WorkerThreadNodes(currNode);

        } //end looping through the set of nodes   


        BufferedWriter bw;
        bw = new BufferedWriter(new FileWriter(javaGEXFoutput, true));
        nodes.append("</nodes>");
        bw.write(nodes.toString());
        //System.out.println(nodeCounter);
        bw.close();

        writeGexfNodes.closeAndPrintClock();


    } // end method createGexfNodes()

    public static void createGexfEdges() throws IOException {

        Clock writeGexfEdges = new Clock("writing the edges into the gexf file");
        BufferedWriter bw;
        bw = new BufferedWriter(new FileWriter(javaGEXFoutput, true));
        bw.newLine();
        bw.write("<edges>\n");
        bw.close();
        HashSet<String> nodesPairs = new HashSet();


        ListIterator<Triple<HashMap<String, String>, Pair<String, String>, LocalDate>> it = listTransactionsAndDates.listIterator();
        System.out.println("Number of transactions: " + listTransactionsAndDates.size());
        int edgeCounter = 0;
        int transactionCounter = 0;

        //iterate through all transactions
        while (it.hasNext()) {
            GUIMain.screen6.jProgressBar1.setValue(Math.round((float) (countNodesLoops + transactionCounter + 1) / (float) (setNodes.size() + listTransactionsAndDates.size()) * 100));
            // this triple records the full transaction, the pair of node of this transaction, and the date of the transaction.
            Triple<HashMap<String, String>, Pair<String, String>, LocalDate> currTrans = it.next();

            //these two lines take the pair of nodes of this current transaction and checks with a boolean whether it is a new one or not
            Pair<String, String> currPair = currTrans.getMiddle();
            boolean newNodesPair = nodesPairs.add(currPair.getLeft().concat(currPair.getRight()));


            //the processus of edge creation is launched only if the transaction deals with a pair of nodes which was has not been already treated
            if (newNodesPair) {

                edgeCounter++;
                //pool.execute(new WorkerThreadEdges(edgeCounter, transactionCounter,currTrans));
                //the processus of edge creation takes needs an edge counter, a transaction counter and the full transaction on which we are currently iterating
                new WorkerThreadEdges(edgeCounter, transactionCounter, currTrans);
                if (edgeCounter % 100 == 0) {
                    System.out.println(edgeCounter);
                }
            }
            transactionCounter++;
        } //end looping through the list of Transactions and their corresponding dates and pairs of nodes   



        bw = new BufferedWriter(new FileWriter(javaGEXFoutput, true));
        edges.append("</edges>\n");
        edges.append("</graph>\n");
        edges.append("</gexf>\n");
        bw.write(edges.toString());
        //System.out.println(nodeCounter);
        bw.close();
        writeGexfEdges.closeAndPrintClock();

    } // end method createGexfNodes()

    static void flushQueue() throws IOException {

        while (!queue.isEmpty()) {

            BufferedWriter bw;
            bw = new BufferedWriter(new FileWriter(javaGEXFoutput, true));
            synchronized (bw) {

                bw.write(queue.poll().toString());
                //System.out.println(nodeCounter);
                bw.close();
            }

        }

    }
} //end public class Main

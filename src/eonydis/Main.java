/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eonydis;

import com.csvreader.CsvReader;
import gui.GUIMain;
import gui.Screen2;
import java.io.*;
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
    String textDelimiter = "\"";
    String fieldDelimiter = ",";
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
    public static HashSet<String> stringAttributes;
    public static Object[] stringAttributesObjects;
    public static HashSet<String> averageAttributes;
    public static HashSet<String> setNodeStaticAttributes;
    public static Object[] averageAttributesObjects;
    public static Object[] staticAttributesObjects;
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
    public static String[] tempAllAttributes;
    public static String[] remainingAttributes;
    public static HashSet<Integer> setIndicesStringEdgeAttributes = new HashSet();
    public static HashSet<Integer> setIndicesStringNodeAttributes = new HashSet();

    public Main(String wk, String file, String doAverage, String fieldDelimiter, String textDelimiter) {

        pathFile = wk + "\\";
        nameFile = file;
        this.doAverage = Boolean.getBoolean(doAverage);
        if (!textDelimiter.equals("")) {
            this.textDelimiter = textDelimiter;
        }
        if (!fieldDelimiter.equals("")) {
            this.fieldDelimiter = fieldDelimiter;
        }
    }

    @Override
    public void run() {
        try {



            javaGEXFoutput = pathFile + fileName + "_eonydis.gexf";
            System.out.println("file name is: " + javaGEXFoutput);
            char textdelimiter = textDelimiter.charAt(0);
            char fielddelimiter = fieldDelimiter.charAt(0);

            transactionsCsv = new CsvReader(new BufferedReader(new FileReader(pathFile + nameFile)), fielddelimiter);
            transactionsCsv.setTextQualifier(textdelimiter);
            transactionsCsv.setUseTextQualifier(true);

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
        Screen2.toBeSelected.setText("<html>select attribute(s) for <b>source nodes</b><br><br><i>use shift and ctrl to select multiple attributes<br>no attributes? just click next</i></html>");
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

        Screen2.toBeSelected.setText("<html>select attribute(s) for <b>target nodes</b><br><br><i>use shift and ctrl to select multiple attributes<br>no attributes? just click next</i></html>");
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

        Screen2.toBeSelected.setText("<html>select one attribute for <b>edge weight</b><br><br><i>no attribute for weight? just click next</i></html>");
        Screen2.count = 5;


        //this creates an array of node attributes made of the attributes of source nodes AND attributes of the target nodes
        nodeAttributes = new String[sourceAttributes.length + targetAttributes.length];

        for (int i = 0; i < sourceAttributes.length; i++) {
            nodeAttributes[i] = sourceAttributes[i];
        }
        for (int i = 0; i < targetAttributes.length; i++) {
            nodeAttributes[i + sourceAttributes.length] = targetAttributes[i];
        }

    }

    public static void selectEdgeWeight() {
        edgeWeight = (String) Screen2.listFields.getSelectedValue();
        System.out.println("edge weight is: " + edgeWeight);

        Screen2.model.removeElement(edgeWeight);

        Screen2.toBeSelected.setText("<html>select other <b>edge attribute(s)</b><br><br><i>use shift and ctrl to select multiple attributes<br>no attributes? just click next</i></html>");
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
        Screen2.toBeSelected.setText("<html>select <b>time field</b></html>");
        Screen2.count = 7;


        //this appendix to SelectEdgeAttributes adds the edge attribute selected for weight (if any) to the end of the list of edge attributes

        if (edgeWeight != null) {
            String[] tempEdgeAttributes = new String[edgeAttributes.length + 1];
            System.arraycopy(edgeAttributes, 0, tempEdgeAttributes, 0, edgeAttributes.length);
            tempEdgeAttributes[tempEdgeAttributes.length - 1] = edgeWeight;
            edgeAttributes = new String[tempEdgeAttributes.length];
            System.arraycopy(tempEdgeAttributes, 0, edgeAttributes, 0, edgeAttributes.length);
        }


        //this appendix collates all selected attributes - for nodes and edges - into a single array
        //this will be useful later for the indexing of which attributes will be Float, and which will be String
        System.out.println("number of edge Attributes is: " + edgeAttributes.length);
        System.out.println("number of node Attributes is: " + nodeAttributes.length);

        tempAllAttributes = new String[edgeAttributes.length + nodeAttributes.length];
        System.arraycopy(edgeAttributes, 0, tempAllAttributes, 0, edgeAttributes.length);
        System.arraycopy(nodeAttributes, 0, tempAllAttributes, edgeAttributes.length, nodeAttributes.length);
        remainingAttributes = new String[Screen2.model.getSize()];
        Screen2.model.copyInto(remainingAttributes);
        Screen2.model.clear();


        for (int i = 0; i < tempAllAttributes.length; i++) {
            Screen2.model.add(i, tempAllAttributes[i]);
        }
        Screen2.toBeSelected.setText("<html>among the attributes you selected,<br> which are <b>static</b>?<br><br><i>static attributes are not time-dependent. For example, if your nodes are persons,<br>this could be an attribute representing their gender or birth date</i><br><br><i>use shift and ctrl to select multiple attributes<br>no attributes? just click next</i>.</html>");

    }

    public static void selectStaticAttributes() {
        staticAttributesObjects = Screen2.listFields.getSelectedValues();
        setNodeStaticAttributes = new HashSet();

        for (int i = 0; i < staticAttributesObjects.length; i++) {
            System.out.println("selected static attribute: " + staticAttributesObjects[i].toString());
            setNodeStaticAttributes.add(staticAttributesObjects[i].toString());

        }


        Screen2.toBeSelected.setText("<html>among the attributes you selected,<br> which are <b>textual</b>?<br><br><i>attributes representing textual information, as opposed to numbers</i>.<br><br><i>use shift and ctrl to select multiple attributes<br>no attributes? just click next</i></html>");

        Screen2.model.clear();


        for (int i = 0; i < tempAllAttributes.length; i++) {
            Screen2.model.add(i, tempAllAttributes[i]);
        }

        Screen2.count = 8;

    }

    public static void selectStringAttributes() {
        stringAttributesObjects = Screen2.listFields.getSelectedValues();
        stringAttributes = new HashSet();

        for (int i = 0; i < stringAttributesObjects.length; i++) {
            System.out.println("selected string attribute: " + stringAttributesObjects[i].toString());
            stringAttributes.add(stringAttributesObjects[i].toString());

        }


        Screen2.toBeSelected.setText("<html>select numerical attributes to be <b>averaged</b><br><br><i>This applies to the case when two attributes are found at the same time.<br> For example, when there are several calls made the same day between two persons.<br>Should the duration of their calls for the day be the sum or the average of each call?<br>By default, values are summed, but you can choose to average them.</i><br><br><i>use shift and ctrl to select multiple attributes<br>no attributes? just click next</i></html>");

        Screen2.model.clear();
        int indexFloatAttribute = 0;
        for (int i = 0; i < tempAllAttributes.length; i++) {

            if (!stringAttributes.contains(tempAllAttributes[i]) & !setNodeStaticAttributes.contains(tempAllAttributes[i])) {
                Screen2.model.add(indexFloatAttribute, tempAllAttributes[i]);
                indexFloatAttribute++;
            }
        }

        Screen2.count = 9;

    }

    public static void selectAverageAttributes() {
        averageAttributesObjects = Screen2.listFields.getSelectedValues();
        averageAttributes = new HashSet();

        for (int i = 0; i < averageAttributesObjects.length; i++) {
            System.out.println("selected attribute(s) to be averaged: " + averageAttributesObjects[i].toString());
            averageAttributes.add(averageAttributesObjects[i].toString());

        }


        Screen2.toBeSelected.setText("<html>select <b>time field</b><br><br><i>The time field is the column in your csv file where the timestamp of each transaction is recorded</i></html>");

        Screen2.model.clear();

        for (int i = 0; i < remainingAttributes.length; i++) {
            Screen2.model.add(i, remainingAttributes[i]);
        }

        Screen2.count = 10;

    }

    public static void selectTimeField() {


        timeField = (String) Screen2.listFields.getSelectedValue();
        System.out.println("time field is: " + timeField);

        Screen2.model.removeElement(timeField);
        Main.screen2.setVisible(false);
        GUIMain.screen4.setVisible(true);

        Screen2.OKButton.setText("<html><b>create gexf</b></html>");
        Screen2.count = 9;

    }

    //reads lines of the csv file and instanciates as many objects from the Transaction class
    public static void populateTransactions() throws IOException {
        Clock readTransactions = new Clock("reading each line of the csv file");

        int indexStartYear = userDefinedTimeFormat.indexOf("y");
        //System.out.println("indexStartYear is: " + indexStartYear);

        int indexStartMonth = userDefinedTimeFormat.indexOf("m");
        //System.out.println("indexStartMonth is: " + indexStartMonth);

        int indexStartDay = userDefinedTimeFormat.indexOf("d");
        //System.out.println("indexStartDay is: " + indexStartDay);

        int indexStartHour = userDefinedTimeFormat.indexOf("h");
        //System.out.println("indexStartHour is: " + indexStartHour);

        int indexStartMinute = userDefinedTimeFormat.indexOf("i");
        //System.out.println("indexStartMinute is: " + indexStartMinute);

        int indexStartSecond = userDefinedTimeFormat.indexOf("s");
        //System.out.println("indexStartSecond is: " + indexStartSecond);

        userDefinedTimeFormat = userDefinedTimeFormat.replace("yyyy", "(\\d*)");
        userDefinedTimeFormat = userDefinedTimeFormat.replace("#", ".");
        userDefinedTimeFormat = userDefinedTimeFormat.replace("mm", "(\\d*)");
        userDefinedTimeFormat = userDefinedTimeFormat.replace("dd", "(\\d*)");
        userDefinedTimeFormat = userDefinedTimeFormat.replace("hh", "(\\d*)");
        userDefinedTimeFormat = userDefinedTimeFormat.replace("ii", "(\\d*)");
        userDefinedTimeFormat = userDefinedTimeFormat.replace("ss", "(\\d*)");
        //System.out.println("regex pattern is: " + userDefinedTimeFormat);


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
            //System.out.println("group number for " + orderOfTimeFieldsInUserPattern[j] + " is " + (j + 1));
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
        if (!setNodeStaticAttributes.isEmpty()) {
            bw.write("    <attributes class=\"node\" mode=\"static\">");
            bw.newLine();
            Iterator<String> setNodeStaticAttributesIterator = setNodeStaticAttributes.iterator();
            while (setNodeStaticAttributesIterator.hasNext()) {
                String currNodeStaticAttribute = setNodeStaticAttributesIterator.next();
                if (stringAttributes.contains(currNodeStaticAttribute)) {
                    bw.write("      <attribute id=\"" + currNodeStaticAttribute + "\" title=\"" + currNodeStaticAttribute + "\" type=\"string\"></attribute>");
                } else {
                    bw.write("      <attribute id=\"" + currNodeStaticAttribute + "\" title=\"" + currNodeStaticAttribute + "\" type=\"float\"></attribute>");
                }
                bw.newLine();
            }
            bw.write("    </attributes>");
            bw.newLine();
        }
        HashSet setNodeDynamicAttributes = new HashSet();
        setNodeDynamicAttributes.addAll(Arrays.asList(Main.sourceAttributes));
        setNodeDynamicAttributes.addAll(Arrays.asList(Main.targetAttributes));
        setNodeDynamicAttributes.removeAll(setNodeStaticAttributes);

        if (!setNodeDynamicAttributes.isEmpty()) {
            bw.write("    <attributes class=\"node\" mode=\"dynamic\">");
            bw.newLine();
            Iterator<String> setNodeDynamicAttributesIterator = setNodeDynamicAttributes.iterator();
            while (setNodeDynamicAttributesIterator.hasNext()) {
                String currNodeDynamicAttribute = setNodeDynamicAttributesIterator.next();
                if (stringAttributes.contains(currNodeDynamicAttribute)) {
                    bw.write("      <attribute id=\"" + currNodeDynamicAttribute + "\" title=\"" + currNodeDynamicAttribute + "\" type=\"string\"></attribute>");
                } else {
                    bw.write("      <attribute id=\"" + currNodeDynamicAttribute + "\" title=\"" + currNodeDynamicAttribute + "\" type=\"float\"></attribute>");
                }
                bw.newLine();
            }
            bw.write("    </attributes>");
            bw.newLine();
        }

        if (edgeAttributes.length != 0) {

            bw.write("    <attributes class=\"edge\" mode=\"dynamic\">");
            bw.newLine();
            if (edgeWeight != null) {
                if (stringAttributes.contains(edgeWeight)) {
                    bw.write("      <attribute id=\"weight\" title=\"Weight\" type=\"string\"></attribute>");
                } else {
                    bw.write("      <attribute id=\"weight\" title=\"Weight\" type=\"float\"></attribute>");
                }

                bw.newLine();
                lengthEdgeAttributesArray = edgeAttributes.length - 1;

            } else {
                lengthEdgeAttributesArray = edgeAttributes.length;
            }
            for (int i = 0; i < lengthEdgeAttributesArray; i++) {
                if (stringAttributes.contains(edgeAttributes[i])) {
                    bw.write("      <attribute id=\"" + edgeAttributes[i] + "\" title=\"" + edgeAttributes[i] + "\" type=\"string\"></attribute>");
                } else {
                    bw.write("      <attribute id=\"" + edgeAttributes[i] + "\" title=\"" + edgeAttributes[i] + "\" type=\"float\"></attribute>");
                }
                bw.newLine();
            }
            bw.write(
                    "    </attributes>");
        }

        bw.newLine();

        bw.write(
                "<nodes>");
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

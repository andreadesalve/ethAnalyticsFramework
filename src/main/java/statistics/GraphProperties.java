/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package statistics;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import dataModel.graphModel.Account;
import dataModel.graphModel.Contract;
import dataModel.graphModel.Interaction;
import dataModel.graphModel.NodeEth;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import util.Convert;
import util.Convert.Unit;

/**
 * Prediction on blockchain timeseries
 * @author andreads
 */
public class GraphProperties {

    /**
     * TRAINING
     */
    //  Trainig/Test set split fraction
    static final double testSplitSize = 0.1;
    /**
     * Proprieta' supportate dalla predizione
     */
    public static final String EDGE_IN_DEGREE = "EDGE_IN_DEGREE";
    public static final String EDGE_OUT_DEGREE = "EDGE_OUT_DEGREE";
    public static final String VERTEX_IN_DEGREE = "VERTEX_IN_DEGREE";
    public static final String VERTEX_OUT_DEGREE = "VERTEX_OUT_DEGREE";
    public static final String ETH_IN_SC = "ETH_IN_SC";
    public static final String ETH_OUT_SC = "ETH_OUT_SC";
    public static final String ETH_IN_EOA = "ETH_IN_EOA";
    public static final String ETH_OUT_EOA = "ETH_OUT_EOA";
    /** Numero di blocchi in un intervallo */
    static int timeslotSize = 50000;
    /** Directory in cui si trovano i dati */
    static String dataDirName = "/home/andreads/NetBeansProjects/EthereumTesting/dataCorrectBesu/";
    /** FIle che contiene timestamp dei blocchi */
    static String timestampTimeFile = "/home/andreads/NetBeansProjects/EthereumTesting/dataBesu/BlockTimeFrom0To4000000";
    /** Type of graph graphType(G1,G2,G3)*/
    static String graphType = "G1";
    /** Numero Max di blocchi del dataset */
    static final int MaxTimeslotSize = 4000000;
    /** Numero di timeslot totali*/
    static int ntimeslot;
    /** Numero di timeslot consecutivi */
    static int timeslotConsecutivi = 50;
    //Smart Contract Temporal Networks
    private static HashMap<String, NodeEth> nodesMap = new HashMap<String, NodeEth>();
    private static DirectedSparseMultigraph<NodeEth, Interaction> graph = null;

    public static void main(String[] args) throws Exception {
        timeslotSize = Integer.valueOf(args[0]);
        graphType = args[1];
        dataDirName = args[2];
        timestampTimeFile = args[3];
//        int InitTimeslot = -1;
//        if (args.length > 4) {
//            InitTimeslot = Integer.valueOf(args[4]);
//        }
        ntimeslot = MaxTimeslotSize / timeslotSize;
        System.out.println("Timeslot size: " + timeslotSize + " blocks");
        System.out.println("Data dir: " + dataDirName);
        System.out.println("Grapht Type: " + dataDirName);
        System.out.println("Numero timeslot: " + ntimeslot);


        //LEGGO tutti i file
        File dataDir = new File(dataDirName);
        if (!(dataDir.isDirectory())) {
            System.out.println("File " + dataDirName + " is not a dir..");
            System.exit(1);
        }
        String[] line;
        File[] files = dataDir.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".csv");
            }
        });

        String[] metrics = new String[]{EDGE_IN_DEGREE, EDGE_OUT_DEGREE, VERTEX_IN_DEGREE, VERTEX_OUT_DEGREE, ETH_IN_SC, ETH_OUT_SC, ETH_IN_EOA, ETH_OUT_EOA};
        //Leggo date
        HashMap<Long, String> t = new HashMap<>();
        CSVReader csvR = new CSVReader(new FileReader(timestampTimeFile), '\t', CSVWriter.DEFAULT_QUOTE_CHARACTER);
        while ((line = csvR.readNext()) != null) {
            long block = Long.valueOf(line[0]);
            t.put(block, line[1]);
        }
        csvR.close();

        //Inizio
        //Fine
        String fileOutName = "GraphStatistics" + graphType + "Delta" + timeslotSize + ".csv";
        CSVWriter w = new CSVWriter(new FileWriter(fileOutName, true), ',', CSVWriter.NO_QUOTE_CHARACTER);
        //w.writeNext(new String[]{
        //            "timeslot",
        //            "date",
        //            "InDegree",
        //            "OutDegree",
        //            "Type"
        //        });
        for (int timeslot = 0, startBlock = 0, finalBlock = timeslotSize - 1;
                timeslot < ntimeslot;
                timeslot++, startBlock += timeslotSize, finalBlock += timeslotSize) {
            //if (InitTimeslot >= 0 && InitTimeslot == timeslot) {
            System.out.println("Load timeslot " + timeslot + " graph from " + startBlock + " to " + finalBlock);
            nodesMap.clear();
            graph = new DirectedSparseMultigraph<NodeEth, Interaction>();
            for (File f : files) {
                String startB = f.getName().substring(f.getName().indexOf('[') + 1, f.getName().indexOf('-'));
                String finalB = f.getName().substring(f.getName().indexOf('-') + 1, f.getName().indexOf(')'));
                System.out.println("File " + f.getName() + " start " + startB + " end " + finalB);
                if (Long.valueOf(finalB) < startBlock
                        || Long.valueOf(startB) > finalBlock) {
                    System.out.println("leave.." + f.getName());
                    continue;
                } else {
                    System.out.println("load.." + f.getName());
                    if (graphType.equals("G1")) {
                        importG1(f, startBlock, finalBlock);
                    } else if (graphType.equals("G2")) {
                        importG2(f, startBlock, finalBlock);
                    } else if (graphType.equals("G3")) {
                        importG3(f, startBlock, finalBlock);
                    }
                }
            }


            for (NodeEth node : graph.getVertices()) {
                HashMap<String, Double> computedVal = computeVal(graph, metrics, node.address);
                ArrayList<String> record = new ArrayList<String>();
                record.add(String.valueOf(timeslot));
                record.add(String.valueOf(t.get((long) timeslot * timeslotSize)));
                record.add(node.isContract ? "SC" : "EOA");
                for (String m : metrics) {
                    record.add(String.valueOf(computedVal.get(m)));
                }
                w.writeNext(record.toArray(new String[]{}));
            }
            nodesMap.clear();
        }
        // }
        w.close();
        //HashMap<Long, HashMap<String, Double>> prepareValToPredict = prepareValToPredict(
        //        loadGraph,
        //        fieldsToForecast);
    }
    /**
     * Aggiorna i dati della struttura con quelli presenti nella linea
     * @param line 
     */
    private static HashSet<String> txHash = new HashSet<String>();
    private static String prevTx = "";

    /**
     * Legge dal file and inport the transaction from startBlock to finalBlock
     * blockNumber,t.getHash,
    contractAddress,
    GasPrice,
    GasLimit,
    Sender,
    Value,
    "Contract Creation","Contract Call"
    gasUsed
     * @param fileName Nome csv che contiene i dati
     * @param startBlock Numero blocco iniziale da caricare
     * @param finalBlock Numero blocco finale da caricare
     * @return Un multigrafo diretto
     * @throws IOException 
     */
    public static DirectedSparseMultigraph<NodeEth, Interaction> importG1(File fileName, int startBlock, int finalBlock) throws IOException {
        CSVReader wr = new CSVReader(new FileReader(fileName), '\t', CSVWriter.DEFAULT_QUOTE_CHARACTER);
        HashSet<String> txHash = new HashSet<String>();
        String[] line;
        int lineNumber = 1;
        while ((line = wr.readNext()) != null) {
            int blockNumber = Integer.valueOf(line[0]);
            String tHash = line[1];
            if (blockNumber >= startBlock && blockNumber <= finalBlock && !txHash.contains(tHash)) {
                Interaction inte = new Interaction();
                String type = line[2];
                String callType = line[3];
                int subtraces = Integer.valueOf(line[4]);
                String gasUsed = line[5];
                String value = line[6];
                String from = line[7];
                boolean isFromContract = line[8].equals("1");

                String to = line[9];
                boolean isToContract = line[10].equals("1");
                inte.setTraceAddress(line[11]);
                inte.setTxsId(tHash);
                inte.setBlockNumber(Integer.valueOf(blockNumber));
//                inte.setSrc(from);
                inte.setContractSrc(isFromContract);
//                inte.setDst(to);
                inte.setContractDst(isToContract);
                //System.out.println(callType+" - "+type);
                inte.setOperation(!callType.isEmpty() ? callType : type);
                inte.setGasUsed(gasUsed);
                inte.setValue(value);
                inte.setSubtraces(subtraces);
                //inte.setTxnFee(gasUsed * gasPrice);

                addVertices(inte, from, to);

                //Aggiungo l'interazione
                graph.addEdge(inte, new Pair<NodeEth>(nodesMap.get(from), nodesMap.get(to)), EdgeType.DIRECTED);
                txHash.add(tHash);
            }
            //System.out.println("Line: " + lineNumber);
            lineNumber++;
        }
        wr.close();

        return graph;
    }

    /**
     * Load Transactions and their internal calls
     * @param fileName
     * @param startBlock
     * @param finalBlock
     * @return
     * @throws IOException 
     */
    private static DirectedSparseMultigraph<NodeEth, Interaction> importG2(File fileName, int startBlock, int finalBlock) throws IOException {
        CSVReader wr = new CSVReader(new FileReader(fileName), '\t', CSVWriter.DEFAULT_QUOTE_CHARACTER);
        String[] line;

        while ((line = wr.readNext()) != null) {
            int blockNumber = Integer.valueOf(line[0]);
            String tHash = line[1];
            if (blockNumber >= startBlock && blockNumber <= finalBlock) {
                Interaction inte = new Interaction();
                //String type = line[2];
                //String callType = line[3];
                //int subtraces = Integer.valueOf(line[4]);
                //String gasUsed = line[5];
                String value = line[6];
                String from = line[7];
                boolean isFromContract = line[8].equals("1");
                String to = line[9];
                boolean isToContract = line[10].equals("1");
                inte.setTraceAddress(line[11]);
                inte.setTxsId(tHash);
                //inte.setBlockNumber(Integer.valueOf(blockNumber));
                if (from.isEmpty() || to.isEmpty()) {
                    System.out.println("Tx " + tHash + " has empty source or destination..");
                    System.exit(1);
                }
//                inte.setSrc(from);
                inte.setContractSrc(isFromContract);
//                inte.setDst(to);
                inte.setContractDst(isToContract);
                //System.out.println(callType+" - "+type);
                //inte.setOperation(!callType.isEmpty() ? callType : type);
                //inte.setGasUsed(gasUsed);
                inte.setValue(value);
                //inte.setSubtraces(subtraces);
                //inte.setTxnFee(gasUsed * gasPrice);
                addVertices(inte, from, to);
                //Aggiungo l'interazione
                graph.addEdge(inte, new Pair<NodeEth>(nodesMap.get(from), nodesMap.get(to)), EdgeType.DIRECTED);
            }

            if (blockNumber % 1000 == 0) {
                System.out.println("Block number: " + blockNumber);
            }

        }
        wr.close();

        return graph;
    }

    private static DirectedSparseMultigraph<NodeEth, Interaction> importG3(File fileName, int startBlock, int finalBlock) throws IOException {
        CSVReader wr = new CSVReader(new FileReader(fileName), '\t', CSVWriter.DEFAULT_QUOTE_CHARACTER);
        String[] line;
        int lineNumber = 1;
        String prevTx = "";
        while ((line = wr.readNext()) != null) {
            int blockNumber = Integer.valueOf(line[0]);
            String tHash = line[1];
            if (blockNumber >= startBlock && blockNumber <= finalBlock) {
                int subtraces = Integer.valueOf(line[4]);
                if (prevTx.equals(tHash)) {
                    //Internal call relativa ad una transazione precedente
                    String type = line[2];
                    String callType = line[3];
                    String gasUsed = line[5];
                    String value = line[6];
                    String from = line[7];
                    boolean isFromContract = line[8].equals("1");
                    String to = line[9];
                    boolean isToContract = line[10].equals("1");
                    Interaction inte = new Interaction();
                    inte.setTraceAddress(line[11]);
                    inte.setTxsId(tHash);
                    inte.setBlockNumber(Integer.valueOf(blockNumber));
//                    inte.setSrc(from);
                    inte.setContractSrc(isFromContract);
//                    inte.setDst(to);
                    inte.setContractDst(isToContract);
                    //System.out.println(callType+" - "+type);
                    inte.setOperation(!callType.isEmpty() ? callType : type);
                    inte.setGasUsed(gasUsed);
                    inte.setValue(value);
                    inte.setSubtraces(subtraces);
                    //inte.setTxnFee(gasUsed * gasPrice);
                    addVertices(inte, from, to);
                    //Aggiungo l'interazione
                    graph.addEdge(inte, new Pair<NodeEth>(nodesMap.get(from), nodesMap.get(to)), EdgeType.DIRECTED);
                } else {
                    //Nuova transazione che potrebbe contenere internal call
                    prevTx = tHash;
                }
            }
            //System.out.println("Line: " + lineNumber);
            lineNumber++;
        }
        wr.close();
        return graph;
    }

    /**
     * Aggiunge i vertici dell'interazione al grafo (e alla mappa), se necessario
     * @param interaction 
     */
    private static void addVertices(Interaction interaction, String from, String to) {
        if (!nodesMap.containsKey(from)) {

            NodeEth n = new NodeEth(from);
            n.setIsContract(interaction.isContractSrc());
            graph.addVertex(n);
            nodesMap.put(from, n);

        }

        if (!nodesMap.containsKey(to)) {
            NodeEth n = new NodeEth(to);
            n.setIsContract(interaction.isContractDst());
            graph.addVertex(n);
            nodesMap.put(to, n);

        }
    }

    /**
     * Calcola i valori dal grafo
     * @param loadGraph
     * @param uid
     */
    private static HashMap<Long, HashMap<String, Double>> prepareValToPredict(HashMap<Long, DirectedSparseMultigraph<NodeEth, Interaction>> loadGraph, String uid, String[] prop) {
        HashMap<Long, HashMap<String, Double>> res = new HashMap<Long, HashMap<String, Double>>();
        for (long ts : loadGraph.keySet()) {
            res.put(ts, new HashMap<String, Double>());
            HashMap<String, Double> computedVal = computeVal(loadGraph.get(ts), prop, uid);
            res.get(ts).putAll(computedVal);
        }
        return res;
    }

    /**
     * Dato un grafo, calcolo tutte le misure relative all'utente
     * @param get
     * @param prop
     * @param uid
     * @return 
     */
    private static HashMap<String, Double> computeVal(DirectedSparseMultigraph<NodeEth, Interaction> get, String[] prop, String uid) {
        HashMap<String, Double> res = new HashMap<>();
        for (String measureName : prop) {
            switch (measureName) {
                case EDGE_IN_DEGREE:
                    res.put(EDGE_IN_DEGREE, (double) get.getInEdges(nodesMap.get(uid)).size());
                    break;
                case EDGE_OUT_DEGREE:
                    res.put(EDGE_OUT_DEGREE, (double) get.getOutEdges(nodesMap.get(uid)).size());
                    break;
                case VERTEX_IN_DEGREE:
                    HashSet<String> inDeg = new HashSet<String>();
                    for (Interaction e : get.getInEdges(nodesMap.get(uid))) {
                        NodeEth source = get.getSource(e);
                        inDeg.add(source.address);
                    }
                    res.put(VERTEX_IN_DEGREE, (double) inDeg.size());
                    break;
                case VERTEX_OUT_DEGREE:
                    HashSet<String> outDeg = new HashSet<String>();
                    for (Interaction e : get.getOutEdges(nodesMap.get(uid))) {
                        NodeEth dest = get.getDest(e);
                        outDeg.add(dest.address);
                    }
                    res.put(VERTEX_OUT_DEGREE, (double) outDeg.size());
                    break;
                case ETH_IN_SC:
                    double tot = 0.0;
                    for (Interaction e : get.getInEdges(nodesMap.get(uid))) {
                        NodeEth source = get.getSource(e);
                        if (source.isContract()) {
                            BigInteger v = new BigInteger(e.getValue().replace("0x", ""), 16);
                            BigDecimal fromWei = Convert.fromWei(v.toString(10), Unit.ETHER);
                            tot += fromWei.doubleValue();
                        }
                    }
                    res.put(ETH_IN_SC, tot);
                    break;
                case ETH_OUT_SC:
                    tot = 0.0;
                    for (Interaction e : get.getOutEdges(nodesMap.get(uid))) {
                        NodeEth dest = get.getDest(e);
                        if (dest.isContract()) {
                            BigInteger v = new BigInteger(e.getValue().replace("0x", ""), 16);
                            BigDecimal fromWei = Convert.fromWei(v.toString(10), Unit.ETHER);
                            tot += fromWei.doubleValue();
                        }
                    }
                    res.put(ETH_OUT_SC, tot);
                    break;
                case ETH_IN_EOA:
                    tot = 0.0;
                    for (Interaction e : get.getInEdges(nodesMap.get(uid))) {
                        NodeEth source = get.getSource(e);
                        if (!source.isContract()) {
                            BigInteger v = new BigInteger(e.getValue().replace("0x", ""), 16);
                            BigDecimal fromWei = Convert.fromWei(v.toString(10), Unit.ETHER);
                            tot += fromWei.doubleValue();
                        }
                    }
                    res.put(ETH_IN_EOA, tot);
                    break;
                case ETH_OUT_EOA:
                    tot = 0.0;
                    for (Interaction e : get.getOutEdges(nodesMap.get(uid))) {
                        NodeEth dest = get.getDest(e);
                        if (!dest.isContract()) {
                            BigInteger v = new BigInteger(e.getValue().replace("0x", ""), 16);
                            BigDecimal fromWei = Convert.fromWei(v.toString(10), Unit.ETHER);
                            tot += fromWei.doubleValue();
                        }
                    }
                    res.put(ETH_OUT_EOA, tot);
                    break;
            }
        }
        return res;
    }
}

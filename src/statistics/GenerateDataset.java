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
import java.io.FilenameFilter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import prediction.UserInfo;
import prediction.WekaTimeseriesEthPredictor;
import util.Convert;
import util.Convert.Unit;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

/**
 * Generate dataset for predictions for each graph type
 * @author andreads
 */
public class GenerateDataset {

    public static final String EDGE_IN_DEGREE = "EDGE_IN_DEGREE";
    public static final String EDGE_OUT_DEGREE = "EDGE_OUT_DEGREE";
    public static final String VERTEX_IN_DEGREE = "VERTEX_IN_DEGREE";
    public static final String VERTEX_OUT_DEGREE = "VERTEX_OUT_DEGREE";
    public static final String ETH_IN_SC = "ETH_IN_SC";
    public static final String ETH_OUT_SC = "ETH_OUT_SC";
    public static final String ETH_IN_EOA = "ETH_IN_EOA";
    public static final String ETH_OUT_EOA = "ETH_OUT_EOA";
    private static String[] fieldsToForecast = new String[]{EDGE_IN_DEGREE,
        EDGE_OUT_DEGREE, VERTEX_IN_DEGREE, VERTEX_OUT_DEGREE, ETH_IN_EOA, ETH_IN_SC, ETH_OUT_EOA, ETH_OUT_SC};
    /** Numero di blocchi in un intervallo */
    static int timeslotSize = 50000;
    /** Directory in cui si trovano i dati */
    static String dataDirName = "/home/andreads/NetBeansProjects/EthereumTesting/dataCorrectBesu/";
    /** FIle che contiene timestamp dei blocchi */
    static String timestampTimeFile = "/home/andreads/NetBeansProjects/EthereumTesting/dataBesu/BlockTimeFrom0To4000000";
    /** Numero Max di blocchi del dataset */
    static final int MaxTimeslotSize = 4000000;
    /** Numero di timeslot totali*/
    static int ntimeslot;
    /** Numero di timeslot consecutivi */
    static int timeslotConsecutivi = 50;
    /** Type of graph graphType(G1,G2,G3)*/
    static String graphType = "G1";

    public static void main(String[] args) {
        initiCLIoption(args);

        ntimeslot = MaxTimeslotSize / timeslotSize;
        System.out.println("Timeslot size: " + timeslotSize + " blocks");
        System.out.println("Data dir: " + dataDirName);
        System.out.println("Timestamp File: " + timestampTimeFile);
        System.out.println("Numero timeslot: " + ntimeslot);







        System.out.println("Compute graph type " + graphType);
        String outputDir = "T" + timeslotSize + File.separator + "dataset" + File.separator + graphType;

        //--------------------------
        //READ USERS FOR PREDICTION
        //--------------------------
        HashMap<String, UserInfo> result = new HashMap<String, UserInfo>();
        //Numero timeslot consecutivi
        usersTimeslot.clear();
        txHash.clear();
        prevTx = "";
        result = readUsersForPrediction(graphType);

        //Per ogni utente creo un record
        System.out.println("Numero utenti trovati: " + result.size());
        for (String uid : result.keySet()) {
            System.out.println(result.get(uid));
        }
        int c = 0;
        for (String uid : result.keySet()) {
            try {
                c++;
                String val = result.get(uid).isSC ? "SC" : "EOA";
                File w = new File(outputDir + File.separator + uid + "-" + val);
                System.out.println("Utenti processati " + c + " di " + result.keySet().size());

                if (!w.exists()) {
                    //--------------------------
                    //LOAD USER GRAPH
                    //--------------------------
                    HashMap<Long, HashMap<String, Double>> prepareValToPredict = loadGraphLight(result, uid, graphType);
                    //System.out.println("User " + uid + " loaded " + loadGraph.size() + " " + loadGraph.keySet().toString());



                    //--------------------------
                    //CREATE DATASET
                    //--------------------------
                    Instances dataset = createDataset(prepareValToPredict, uid, fieldsToForecast);
                    System.out.println("Dataset " + dataset.toSummaryString());
                    System.out.println("Dataset " + dataset.toString());
                    System.out.println("Scrivo dataset...");
                    ArffSaver s = new ArffSaver();
                    s.setInstances(dataset);
                    s.setFile(w);
                    s.writeBatch();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }


    }

    /**
     * Create the dataset
     * @param loadGraph
     * @return 
     */
    private static Instances createDataset(HashMap<Long, HashMap<String, Double>> prepareValToPredict, String uid, String[] prop) {
        //Creo dataset 
        ArrayList<Attribute> attInfo = new ArrayList<Attribute>();
        Attribute timestamp = new Attribute("data", "dd/MM/yyyy");


        attInfo.add(timestamp);
        for (String p : prop) {
            Attribute centrality = new Attribute(p);
            attInfo.add(centrality);
        }

        Instances dataset = new Instances("id", attInfo, 0);
        try {
            HashMap<Long, String> t = new HashMap<>();
            SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
            //Leggo date
            String[] line;
            CSVReader csvR = new CSVReader(new FileReader(timestampTimeFile), '\t', CSVWriter.DEFAULT_QUOTE_CHARACTER);
            while ((line = csvR.readNext()) != null) {
                long block = Long.valueOf(line[0]);
                t.put(block, line[1]);
            }
            csvR.close();

            //Creo dataset
            for (long ts : prepareValToPredict.keySet()) {
                Instance record = new DenseInstance(attInfo.size());
                record.setValue(timestamp, Long.valueOf(t.get(ts * timeslotSize)));
                for (Attribute att : attInfo) {
                    if (prepareValToPredict.get(ts).containsKey(att.name())) {
                        record.setValue(att, prepareValToPredict.get(ts).get(att.name()));
                    }
                }
                dataset.add(record);
            }
            t.clear();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return dataset;
    }

    public static HashMap<String, UserInfo> readUsersForPrediction(String graphType) {
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
        for (File f : files) {
            try {
                CSVReader csvR = new CSVReader(new FileReader(f), '\t', CSVWriter.DEFAULT_QUOTE_CHARACTER);
                while ((line = csvR.readNext()) != null) {
                    updateData(line, graphType);
                }
                System.out.println(usersTimeslot.keySet());
                csvR.close();
            } catch (IOException ex) {
                System.out.println("File " + f.getAbsolutePath() + " not found..");
            }
        }

        //Calcolo i timeslots in cui l'utente e' attivo 
        //UserID -> TimeintervalAttivi
        HashMap<String, ArrayList<Long>> usersActiveSlot = new HashMap<>();
        for (long ts : usersTimeslot.keySet()) {
            HashMap<String, Boolean> get = usersTimeslot.get(ts);
            for (String user : get.keySet()) {
                if (!usersActiveSlot.containsKey(user)) {
                    usersActiveSlot.put(user, new ArrayList<Long>());
                }
                usersActiveSlot.get(user).add(ts);
            }
        }


        HashMap<String, UserInfo> result = new HashMap<String, UserInfo>();
        System.out.println("Numero time slot consecutivi: " + timeslotConsecutivi);
        //Calcolo il numero di utenti che hanno n timeslot consecutivi   
        for (String uid : usersActiveSlot.keySet()) {
            ArrayList<Long> get = usersActiveSlot.get(uid);
            if (get.size() < timeslotConsecutivi) {
                continue;
            } else if (get.size() >= timeslotConsecutivi && timeslotConsecutivi == 1) {
                //utente uid e' attivo almeno n timeslot
                result.put(uid, new UserInfo(uid, usersTimeslot.get(get.get(0)).get(uid), get.get(0), get.get(0) + timeslotConsecutivi - 1));
            } else {
                Collections.sort(get);
                int contaConsecutivi = 1;
                for (int i = 1; i < get.size(); i++) {
                    if (get.get(i) == get.get(i - 1) + 1) {
                        contaConsecutivi++;
                    } else {
                        contaConsecutivi = 1;
                    }

                    if (contaConsecutivi == timeslotConsecutivi) {
                        result.put(uid, new UserInfo(uid, usersTimeslot.get(get.get(0)).get(uid), get.get(i) - timeslotConsecutivi + 1, get.get(i)));
                        break;
                    }
                }
            }
        }
        usersActiveSlot.clear();
        return result;
    }
    //Timeslot -> UID Presenti -> true if UID=SC false if UID=EOA
    private static HashMap<Long, HashMap<String, Boolean>> usersTimeslot = new HashMap<>();
    private static HashSet<String> txHash = new HashSet<String>();
    private static String prevTx = "";

    /**
     * Aggiungo utente a timeslot
     */
    private static void updateData(String[] line, String graphType) {
        int blockNumber = Integer.valueOf(line[0]);
        String tHash = line[1];
        long timeslotIndex = Math.round(Math.floor((double) blockNumber / (double) MaxTimeslotSize * (double) ntimeslot));
        if (!usersTimeslot.containsKey(timeslotIndex)) {
            usersTimeslot.put(timeslotIndex, new HashMap<String, Boolean>());
        }
        //System.out.println("Blocco " + blockNumber + " belongs to timeslot " + timeslotIndex);
        if (graphType.equals("G1")) {
            if (!txHash.contains(tHash)) {
                String from = line[7];
                boolean isFromContract = line[8].equals("1");
                usersTimeslot.get(timeslotIndex).putIfAbsent(from, isFromContract);
                String to = line[9];
                boolean isToContract = line[10].equals("1");
                usersTimeslot.get(timeslotIndex).putIfAbsent(to, isToContract);
                txHash.add(tHash);
            }
        } else if (graphType.equals("G2")) {
            String from = line[7];
            boolean isFromContract = line[8].equals("1");
            usersTimeslot.get(timeslotIndex).putIfAbsent(from, isFromContract);
            String to = line[9];
            boolean isToContract = line[10].equals("1");
            usersTimeslot.get(timeslotIndex).putIfAbsent(to, isToContract);
        } else if (graphType.equals("G3")) {
            if (prevTx.equals(tHash)) {
                //Internal call relativa ad una transazione precedente
                String from = line[7];
                boolean isFromContract = line[8].equals("1");
                usersTimeslot.get(timeslotIndex).putIfAbsent(from, isFromContract);
                String to = line[9];
                boolean isToContract = line[10].equals("1");
                usersTimeslot.get(timeslotIndex).putIfAbsent(to, isToContract);
            } else {
                //Nuova transazione che potrebbe contenere internal call
                prevTx = tHash;
            }
        }
    }

    /**
     * Command line option
     */
    private static void initiCLIoption(String[] args) {

        Option timeslotSizeOption = Option.builder("timeslotSize").argName("value").required().hasArg().desc("Number of consecutive blocks in each time slot").build();
        Option dataDirNameOption = Option.builder("dataDirName").argName("path").hasArg().required().desc("Directory of the dataset").build();
        Option graphTypeOption = Option.builder("graphType").argName("type").hasArg().required().desc("The type of graph to load (G1,G2,G3)").build();

        Option timestampTimeFileOption = Option.builder("timestampFile").argName("file").hasArg().required().desc("use given file for blocks timestamp").build();
        Option help = new Option("help", "print this message");
        // create the parser
        Options options = new Options();
        options.addOption(help);
        options.addOption(timeslotSizeOption);
        options.addOption(graphTypeOption);
        options.addOption(dataDirNameOption);
        options.addOption(timestampTimeFileOption);
        CommandLineParser parser = new DefaultParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("timeslotSize")) {
                // initialise the member variable
                timeslotSize = Integer.valueOf(line.getOptionValue("timeslotSize"));
            }

            if (line.hasOption("dataDirName")) {
                // initialise the member variable
                dataDirName = line.getOptionValue("dataDirName");
            }

            if (line.hasOption("timestampFile")) {
                // initialise the member variable
                timestampTimeFile = line.getOptionValue("timestampFile");
            }

            if (line.hasOption("graphType")) {
                // initialise the member variable
                graphType = line.getOptionValue("graphType");
            }
        } catch (ParseException exp) {
            // oops, something went wrong
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java prediction.WekaTimeseriesEthPredictor", options);
            System.exit(1);
        }
//     timeslotSize = Integer.valueOf(args[0]);
        //       graphType = args[1];
        //      dataDirName = args[2];
        //      timestampTimeFile=args[3];

    }
    private static HashMap<String, NodeEth> nodesMap = new HashMap<String, NodeEth>();
    /*
     * Carico  dati degl utenti dai grafi
     */

    private static HashMap<Long, HashMap<String, Double>> loadGraphLight(HashMap<String, UserInfo> result, String uid, String graphType) {
        //per ogni file

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


        //System.out.println("Load " + result.get(uid) + " " + result.get(uid).startTimeSlot * timeslotSize + " - " + ((result.get(uid).endTimeSlot + 1) * timeslotSize - 1));
        prevTx = "";
        txHash.clear();
        HashMap<Long, HashMap<String, Double>> prepareValToPredict = new HashMap<Long, HashMap<String, Double>>();

        for (long timeslot = result.get(uid).startTimeSlot;
                timeslot <= result.get(uid).endTimeSlot;
                timeslot++) {
            nodesMap.clear();
            //System.out.println("Load timeslot " + timeslot + " of " + uid);
            //System.out.println("from block " + timeslot * timeslotSize + " to block " + ((timeslot + 1) * timeslotSize - 1));
            DirectedSparseMultigraph<NodeEth, Interaction> graph = new DirectedSparseMultigraph<NodeEth, Interaction>();
            for (File f : files) {
                try {
                    String startB = f.getName().substring(f.getName().indexOf('[') + 1, f.getName().indexOf('-'));
                    String finalB = f.getName().substring(f.getName().indexOf('-') + 1, f.getName().indexOf(')'));
                    //System.out.println("File " + f.getName() + " start " + startB + " end " + finalB);
                    if (Long.valueOf(finalB) < timeslot * timeslotSize
                            || Long.valueOf(startB) > (timeslot + 1) * timeslotSize - 1) {
                        //System.out.println("leave.." + f.getName());
                        continue;
                    } else {
                        CSVReader csvR = new CSVReader(new FileReader(f), '\t', CSVWriter.DEFAULT_QUOTE_CHARACTER);
                        while ((line = csvR.readNext()) != null) {
                            int blockNumber = Integer.valueOf(line[0]);
                            long timeslotIndex = Math.round(Math.floor((double) blockNumber / (double) MaxTimeslotSize * (double) ntimeslot));

                            if (blockNumber >= timeslot * timeslotSize
                                    && blockNumber <= (timeslot + 1) * timeslotSize - 1) {
                                //carico il record nel grafo approprito ed esco dal loop

                                if (graphType.equals("G1")) {
                                    importG1(line, graph);
                                } else if (graphType.equals("G2")) {
                                    importG2(line, graph);
                                } else if (graphType.equals("G3")) {
                                    importG3(line, graph);
                                }
                            }
                        }
                        csvR.close();
                    }
                } catch (IOException ex) {
                    System.out.println("File " + f.getAbsolutePath() + " not found..");
                }
            }

            //--------------------------
            //COMPUTE VALUE TO PREDICT
            //--------------------------
            prepareValToPredict.put(timeslot, prepareValToPredict(
                    graph,
                    uid,
                    fieldsToForecast));

        }
        return prepareValToPredict;
    }

    /**
     * Calcola i valori dal grafo
     * @param loadGraph
     * @param uid
     * @param EDGE_IN_DEGREE 
     */
    private static HashMap<String, Double> prepareValToPredict(DirectedSparseMultigraph<NodeEth, Interaction> loadGraph, String uid, String[] prop) {
        HashMap<String, Double> r = new HashMap<String, Double>();
        for (String measureName : prop) {
            switch (measureName) {
                case EDGE_IN_DEGREE:
                    r.put(EDGE_IN_DEGREE, (double) loadGraph.getInEdges(nodesMap.get(uid)).size());
                    break;
                case EDGE_OUT_DEGREE:
                    r.put(EDGE_OUT_DEGREE, (double) loadGraph.getOutEdges(nodesMap.get(uid)).size());
                    break;
                case VERTEX_IN_DEGREE:
                    HashSet<String> inDeg = new HashSet<String>();
                    for (Interaction e : loadGraph.getInEdges(nodesMap.get(uid))) {
                        NodeEth source = loadGraph.getSource(e);
                        inDeg.add(source.address);
                    }
                    r.put(VERTEX_IN_DEGREE, (double) inDeg.size());
                    break;
                case VERTEX_OUT_DEGREE:
                    HashSet<String> outDeg = new HashSet<String>();
                    for (Interaction e : loadGraph.getOutEdges(nodesMap.get(uid))) {
                        NodeEth dest = loadGraph.getDest(e);
                        outDeg.add(dest.address);
                    }
                    r.put(VERTEX_OUT_DEGREE, (double) outDeg.size());
                    break;
                case ETH_IN_SC:
                    double tot = 0.0;
                    for (Interaction e : loadGraph.getInEdges(nodesMap.get(uid))) {
                        NodeEth source = loadGraph.getSource(e);
                        if (source.isContract()) {
                            BigInteger v = new BigInteger(e.getValue().replace("0x", ""), 16);
                            BigDecimal fromWei = Convert.fromWei(v.toString(10), Unit.ETHER);
                            tot += fromWei.doubleValue();
                        }
                    }
                    r.put(ETH_IN_SC, tot);
                    break;
                case ETH_OUT_SC:
                    tot = 0.0;
                    for (Interaction e : loadGraph.getOutEdges(nodesMap.get(uid))) {
                        NodeEth dest = loadGraph.getDest(e);
                        if (dest.isContract()) {
                            BigInteger v = new BigInteger(e.getValue().replace("0x", ""), 16);
                            BigDecimal fromWei = Convert.fromWei(v.toString(10), Unit.ETHER);
                            tot += fromWei.doubleValue();
                        }
                    }
                    r.put(ETH_OUT_SC, tot);
                    break;
                case ETH_IN_EOA:
                    tot = 0.0;
                    for (Interaction e : loadGraph.getInEdges(nodesMap.get(uid))) {
                        NodeEth source = loadGraph.getSource(e);
                        if (!source.isContract()) {
                            BigInteger v = new BigInteger(e.getValue().replace("0x", ""), 16);
                            BigDecimal fromWei = Convert.fromWei(v.toString(10), Unit.ETHER);
                            tot += fromWei.doubleValue();
                        }
                    }
                    r.put(ETH_IN_EOA, tot);
                    break;
                case ETH_OUT_EOA:
                    tot = 0.0;
                    for (Interaction e : loadGraph.getOutEdges(nodesMap.get(uid))) {
                        NodeEth dest = loadGraph.getDest(e);
                        if (!dest.isContract()) {
                            BigInteger v = new BigInteger(e.getValue().replace("0x", ""), 16);
                            BigDecimal fromWei = Convert.fromWei(v.toString(10), Unit.ETHER);
                            tot += fromWei.doubleValue();
                        }
                    }
                    r.put(ETH_OUT_EOA, tot);
                    break;
            }
        }
        return r;
    }

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
    public static DirectedSparseMultigraph<NodeEth, Interaction> importG1(String[] line, DirectedSparseMultigraph<NodeEth, Interaction> graph) {
        int blockNumber = Integer.valueOf(line[0]);
        String tHash = line[1];
        if (!txHash.contains(tHash)) {
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
            inte.setSrc(from);
            inte.setContractSrc(isFromContract);
            inte.setDst(to);
            inte.setContractDst(isToContract);
            //System.out.println(callType+" - "+type);
            inte.setOperation(!callType.isEmpty() ? callType : type);
            inte.setGasUsed(gasUsed);
            inte.setValue(value);
            inte.setSubtraces(subtraces);
            //inte.setTxnFee(gasUsed * gasPrice);

            addVertices(inte, graph);

            //Aggiungo l'interazione
            graph.addEdge(inte, new Pair<NodeEth>(nodesMap.get(inte.getSrc()), nodesMap.get(inte.getDst())), EdgeType.DIRECTED);
            txHash.add(tHash);
        }
        return graph;
    }

    /**
     * Aggiunge i vertici dell'interazione al grafo (e alla mappa), se necessario
     * @param interaction 
     */
    private static void addVertices(Interaction interaction, DirectedSparseMultigraph<NodeEth, Interaction> graph) {
        if (!nodesMap.containsKey(interaction.getSrc())) {

            NodeEth n = new Contract(interaction.getSrc());
            n.setIsContract(interaction.isContractSrc());
            graph.addVertex(n);
            nodesMap.put(interaction.getSrc(), n);

        }

        if (!nodesMap.containsKey(interaction.getDst())) {
            NodeEth n = new Account(interaction.getDst());
            n.setIsContract(interaction.isContractDst());
            graph.addVertex(n);
            nodesMap.put(interaction.getDst(), n);

        }
    }

    /**
     * Load Transactions and their internal calls
     * @param fileName
     * @param startBlock
     * @param finalBlock
     * @return
     * @throws IOException 
     */
    private static DirectedSparseMultigraph<NodeEth, Interaction> importG2(String[] line, DirectedSparseMultigraph<NodeEth, Interaction> graph) {
        int blockNumber = Integer.valueOf(line[0]);
        String tHash = line[1];
        Interaction inte = new Interaction();
        String type = line[2];
        String callType = line[3];
        int subtraces = Integer.valueOf(line[4]);
        //String gasUsed = line[5];
        String value = line[6];
        String from = line[7];
        boolean isFromContract = line[8].equals("1");
        String to = line[9];
        boolean isToContract = line[10].equals("1");
        inte.setTraceAddress(line[11]);
        inte.setTxsId(tHash);
        inte.setBlockNumber(Integer.valueOf(blockNumber));
        if (from.isEmpty() || to.isEmpty()) {
            System.out.println("Tx " + tHash + " has empty source or destination..");
            System.exit(1);
        }
        inte.setSrc(from);
        inte.setContractSrc(isFromContract);
        inte.setDst(to);
        inte.setContractDst(isToContract);
        //System.out.println(callType+" - "+type);
        //inte.setOperation(!callType.isEmpty() ? callType : type);87ò






        //inte.setGasUsed(gasUsed);
        inte.setValue(value);
        //inte.setSubtraces(subtraces);
        //inte.setTxnFee(gasUsed * gasPrice);
        addVertices(inte, graph);
        //Aggiungo l'interazione
        graph.addEdge(inte, new Pair<NodeEth>(nodesMap.get(inte.getSrc()), nodesMap.get(inte.getDst())), EdgeType.DIRECTED);
        return graph;
    }

    private static DirectedSparseMultigraph<NodeEth, Interaction> importG3(String[] line, DirectedSparseMultigraph<NodeEth, Interaction> graph) {
        int blockNumber = Integer.valueOf(line[0]);
        String tHash = line[1];
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
            inte.setSrc(from);
            inte.setContractSrc(isFromContract);
            inte.setDst(to);
            inte.setContractDst(isToContract);
            //System.out.println(callType+" - "+type);
            inte.setOperation(!callType.isEmpty() ? callType : type);
            inte.setGasUsed(gasUsed);
            inte.setValue(value);
            inte.setSubtraces(subtraces);
            //inte.setTxnFee(gasUsed * gasPrice);
            addVertices(inte, graph);
            //Aggiungo l'interazione
            graph.addEdge(inte, new Pair<NodeEth>(nodesMap.get(inte.getSrc()), nodesMap.get(inte.getDst())), EdgeType.DIRECTED);
        } else {
            //Nuova transazione che potrebbe contenere internal call
            prevTx = tHash;
        }
        return graph;
    }
}

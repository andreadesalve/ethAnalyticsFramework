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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.collections15.Transformer;

/**
 * 
 * @author Andrea De Salve
 */
public class TemporalSmartContractStatistics {

    //Smart Contract Temporal Networks
    private static DirectedSparseMultigraph<NodeEth, Interaction> graph = null;
    private static HashMap<String, NodeEth> nodesMap = new HashMap<String, NodeEth>();

//    /**
//     * Classe per calcolo dei pesi sugli archi
//     */
//    private static class GraphTransformer implements Transformer<Interaction, Double> {
//
//        public Double transform(Interaction input) {
//            NodeEth source = graph.getSource(input);
//            double sum = 0.0;
//            for (Interaction e : graph.getOutEdges(source)) {
//                sum += e.getWeight();
//            }
//
//            return input.getWeight() / sum;
//        }
//    }
    private int temporalWindow;
    private int numberoTotaleBlocchi;

    /**
     * Calcolo HITS centrality sul grafo
     */
    public static void main(String[] args) {

        if (args.length != 5) {
            System.out.println("USAGE: TemporalSmartContractHITS graphType(G1,G2,G3) fileName startBlock finalBlock outFileName");
            System.exit(1);
        }

        String graphType = args[0];
        String fileName = args[1];
        int startBlock = Integer.valueOf(args[2]);
        int finalBlock = Integer.valueOf(args[3]);
        String outFileName = args[4];



        try {
            //Carico Grafo
            System.out.println("Load graph " + graphType + " in " + fileName + " from " + startBlock + " to " + finalBlock);
            graph = new DirectedSparseMultigraph<NodeEth, Interaction>();

            if (graphType.equals("G1")) {
                graph = importG1(fileName, startBlock, finalBlock);
            } else if (graphType.equals("G2")) {
                graph = importG2(fileName, startBlock, finalBlock);
            } else if (graphType.equals("G3")) {
                graph = importG3(fileName, startBlock, finalBlock);
            }
            System.out.println("Statistiche rete: nodes " + graph.getVertexCount() + " edges " + graph.getEdgeCount());
            //Numero smart contract
            int scNum = 0;
            //Numero di EOA
            int eoaNum = 0;
            //Numero Contract Creation
            int txnCC = 0;
            //Numero Contract Call
            int txnCall = 0;
            for (NodeEth node : graph.getVertices()) {
                if (node.isContract()) {
                    scNum++;
                } else {
                    eoaNum++;
                }
            }
            for (Interaction i : graph.getEdges()) {
                if (i.isContractCreation()) {
                    txnCC++;
                } else {
                    txnCall++;
                }
            }
            System.out.println("SC " + scNum + " EOA " + eoaNum + " Contract Creation " + txnCC + " contract call " + txnCall);
            System.out.println("Calcolo centralita");

            System.out.println("Scrivo centralita su " + outFileName);
            CSVWriter w = new CSVWriter(new FileWriter(outFileName), ',', CSVWriter.NO_QUOTE_CHARACTER);
            for (NodeEth node : graph.getVertices()) {
                double inDegree = graph.getInEdges(node).size();
                double outDegree = graph.getOutEdges(node).size();
                w.writeNext(new String[]{
                            node.getAddress(),
                            String.valueOf(node.isContract()),
                            String.valueOf(inDegree),
                            String.valueOf(outDegree)
                        });
            }
            w.close();
        } catch (IOException ex) {
            Logger.getLogger(TemporalSmartContractStatistics.class.getName()).log(Level.SEVERE, null, ex);
        }

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
    public static DirectedSparseMultigraph<NodeEth, Interaction> importG1(String fileName, int startBlock, int finalBlock) throws IOException {
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

                addVertices(inte);

                //Aggiungo l'interazione
                graph.addEdge(inte, new Pair<NodeEth>(nodesMap.get(inte.getSrc()), nodesMap.get(inte.getDst())), EdgeType.DIRECTED);
                txHash.add(tHash);
            }
            System.out.println("Line: " + lineNumber);
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
    private static DirectedSparseMultigraph<NodeEth, Interaction> importG2(String fileName, int startBlock, int finalBlock) throws IOException {
        CSVReader wr = new CSVReader(new FileReader(fileName), '\t', CSVWriter.DEFAULT_QUOTE_CHARACTER);
        String[] line;
        int lineNumber = 1;
        while ((line = wr.readNext()) != null) {
            int blockNumber = Integer.valueOf(line[0]);
            String tHash = line[1];
            if (blockNumber >= startBlock && blockNumber <= finalBlock) {
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
                addVertices(inte);
                //Aggiungo l'interazione
                graph.addEdge(inte, new Pair<NodeEth>(nodesMap.get(inte.getSrc()), nodesMap.get(inte.getDst())), EdgeType.DIRECTED);
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
    private static void addVertices(Interaction interaction) {
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

    private static DirectedSparseMultigraph<NodeEth, Interaction> importG3(String fileName, int startBlock, int finalBlock) throws FileNotFoundException, IOException {
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
                    addVertices(inte);
                    //Aggiungo l'interazione
                    graph.addEdge(inte, new Pair<NodeEth>(nodesMap.get(inte.getSrc()), nodesMap.get(inte.getDst())), EdgeType.DIRECTED);
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
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package prediction;

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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import weka.classifiers.evaluation.NumericPrediction;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.timeseries.WekaForecaster;
import weka.classifiers.timeseries.eval.ErrorModule;
import weka.classifiers.timeseries.eval.TSEvalModule;
import weka.classifiers.timeseries.eval.TSEvaluation;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;

/**
 * Prediction on blockchain timeseries
 * @author andreads
 */
public class TestWekaTimeseriesEthPredictor {

    /**
     * TRAINING
     */
    //  Trainig/Test set split fraction
    static final double testSplitSize = 0.1;
    /**
     * Proprieta' supportate dalla predizione
     */
    public static final int EDGE_IN_DEGREE = 0;
    public static final int EDGE_OUT_DEGREE = 1;
    public static final int VERTEX_IN_DEGREE = 2;
    public static final int VERTEX_OUT_DEGREE = 3;
    /** Numero di blocchi in un intervallo */
    static int timeslotSize = 50000;
    /** Directory in cui si trovano i dati */
    static String dataDirName = "/home/andreads/NetBeansProjects/EthereumTesting/dataCorrectBesu/";
    /** FIle che contiene timestamp dei blocchi */
    static String timestampTimeFile = "/home/andreads/NetBeansProjects/EthereumTesting/dataBesu/BlockTimeFrom0To4000000";
    /** Type of graph graphType(G1,G2,G3)*/
    static String graphType = "G1";
    //Timeslot -> UID Presenti -> true if UID=SC false if UID=EOA
    private static HashMap<Long, HashMap<String, Boolean>> usersTimeslot = new HashMap<>();
    /** Numero Max di blocchi del dataset */
    static final int MaxTimeslotSize = 4000000;
    /** Numero di timeslot totali*/
    static int ntimeslot;
    /** Numero di timeslot consecutivi */
    static int timeslotConsecutivi = 50;
    //Smart Contract Temporal Networks
    private static HashMap<String, NodeEth> nodesMap = new HashMap<String, NodeEth>();

    public static void main(String[] args) throws Exception {
        ntimeslot = MaxTimeslotSize / timeslotSize;
        System.out.println("Timeslot size: " + timeslotSize + " blocks");
        System.out.println("Data dir: " + dataDirName);
        System.out.println("Grapht Type: " + dataDirName);
        System.out.println("Numero timeslot: " + ntimeslot);


        CSVLoader csvLoad = new CSVLoader();
        csvLoad.setDateAttributes("1");
        csvLoad.setDateFormat("dd/MM/yyyy");
        csvLoad.setSource(new File("/home/andreads/NetBeansProjects/WekaEthPrediction/dataExample.csv"));
        Instances dataset = csvLoad.getDataSet();
        System.out.println("Dataset " + dataset.toSummaryString());
        System.out.println("Dataset " + dataset.toString());


        LinearRegression function = new LinearRegression();
        //function.setOutputAdditionalStats(true);
        //System.out.println(Arrays.toString(function.getOptions()));


        WekaForecaster forecaster = new WekaForecaster();
        forecaster.setBaseForecaster(function);
        forecaster.getTSLagMaker().setTimeStampField("data"); // date time stamp
        forecaster.setConfidenceLevel(0.95);
        forecaster.setFieldsToForecast("val");
        // forecaster.getTSLagMaker().setDebug(true);
        // forecaster.setConfidenceLevel(0.95);
        System.out.println("Weka Forecaster option:" + Arrays.toString(forecaster.getOptions()));
        System.out.println("Weka LagMaker option:" + Arrays.toString(forecaster.getTSLagMaker().getOptions()));





        TSEvaluation eval = new TSEvaluation(dataset, testSplitSize);
        System.out.println("Timestamp Field: " + forecaster.getTSLagMaker().getTimeStampField());
        System.out.println("Fields To Forecast: " + forecaster.getFieldsToForecast());
        System.out.println("Training set size:" + eval.getTrainingData().size());
        System.out.println("Test set size:" + eval.getTestData().size());
        System.out.println("Build forecastr..");
        //forecaster.buildForecaster(eval.getTrainingData(), System.out);
        //forecaster.primeForecaster(dataset);
        //System.out.println("WF " + forecaster.toString());
        //forecaster.buildForecaster(eval.getTrainingData(), System.out);
        System.out.println("Future forecast: " + eval.getForecastFuture());
        System.out.println("PrimeForTestDataWithTestData: " + eval.getPrimeForTestDataWithTestData());
        System.out.println("PrimeForTestDataWithTestData: " + eval.getPrimeWindowSize());
        eval.setEvaluationModules("MAE,MSE,RMSE,MAPE,DAC,RAE,RRSE");
        //eval.setEvaluateOnTrainingData(true);
        eval.setRebuildModelAfterEachTestForecastStep(false);
        //eval.setEvaluateOnTestData(true);
        //eval.setPrimeForTestDataWithTestData(false);
        //eval.setForecastFuture(true);
        eval.setPrimeWindowSize(12);

        System.out.println("getPrimeForTestDataWithTestData: " + eval.getPrimeForTestDataWithTestData());
        System.out.println("getPrimeWindowSize: " + eval.getPrimeWindowSize());
        System.out.println("getEvaluateOnTestData: " + eval.getEvaluateOnTestData());
        System.out.println("getEvaluateOnTrainingData: " + eval.getEvaluateOnTrainingData());
        System.out.println("getForecastFuture: " + eval.getForecastFuture());
        System.out.println("getAlgorithmName: " + forecaster.getAlgorithmName());
        System.out.println("getFieldsToForecast: " + forecaster.getFieldsToForecast());
        System.out.println("getOverlayFields: " + forecaster.getOverlayFields());
        System.out.println("getCalculateConfIntervalsForForecasts: " + forecaster.getCalculateConfIntervalsForForecasts());
        System.out.println("getConfidenceLevel: " + forecaster.getConfidenceLevel());
        System.out.println("getFieldsToLagAsString: " + forecaster.getTSLagMaker().getFieldsToLagAsString());
        System.out.println("getLagRange: " + forecaster.getTSLagMaker().getLagRange());
        System.out.println("getPrimaryPeriodicFieldName: " + forecaster.getTSLagMaker().getPrimaryPeriodicFieldName());
        System.out.println("getSkipEntries: " + forecaster.getTSLagMaker().getSkipEntries());
        System.out.println("getTimeStampField: " + forecaster.getTSLagMaker().getTimeStampField());
        System.out.println("getAddAMIndicator: " + forecaster.getTSLagMaker().getAddAMIndicator());
        System.out.println("getAddDayOfMonth: " + forecaster.getTSLagMaker().getAddDayOfMonth());
        System.out.println("getAddDayOfWeek: " + forecaster.getTSLagMaker().getAddDayOfWeek());
        System.out.println("getAddMonthOfYear: " + forecaster.getTSLagMaker().getAddMonthOfYear());
        System.out.println("getAddNumDaysInMonth: " + forecaster.getTSLagMaker().getAddNumDaysInMonth());
        System.out.println("getAddQuarterOfYear: " + forecaster.getTSLagMaker().getAddQuarterOfYear());
        System.out.println("getAddWeekendIndicator: " + forecaster.getTSLagMaker().getAddWeekendIndicator());
        System.out.println("getAdjustForTrends: " + forecaster.getTSLagMaker().getAdjustForTrends());
        System.out.println("getAdjustForVariance: " + forecaster.getTSLagMaker().getAdjustForVariance());
        //System.out.println("getArtificialTimeStartValue: " + forecaster.getTSLagMaker().getArtificialTimeStartValue());
        System.out.println("getAverageConsecutiveLongLags: " + forecaster.getTSLagMaker().getAverageConsecutiveLongLags());
        System.out.println("getAverageLagsAfter: " + forecaster.getTSLagMaker().getAverageLagsAfter());
        System.out.println("getDeltaTime: " + forecaster.getTSLagMaker().getDeltaTime());
        System.out.println("getIncludePowersOfTime: " + forecaster.getTSLagMaker().getIncludePowersOfTime());
        System.out.println("getIncludeTimeLagProducts: " + forecaster.getTSLagMaker().getIncludeTimeLagProducts());
        System.out.println("getMaxLag: " + forecaster.getTSLagMaker().getMaxLag());
        System.out.println("getMinLag: " + forecaster.getTSLagMaker().getMinLag());
        System.out.println("getNumConsecutiveLongLagsToAverage: " + forecaster.getTSLagMaker().getNumConsecutiveLongLagsToAverage());
        System.out.println("getPeriodicity: " + forecaster.getTSLagMaker().getPeriodicity().toString());
        System.out.println("getRemoveLeadingInstancesWithUnknownLagValues: " + forecaster.getTSLagMaker().getRemoveLeadingInstancesWithUnknownLagValues());
        eval.evaluateForecaster(forecaster, true, System.out);
        System.out.println("WF " + forecaster.toString());
        ErrorModule predictionsForTestData = eval.getPredictionsForTestData(1);
        System.out.println("Error Module: " + Arrays.toString(predictionsForTestData.calculateMeasure()));
        System.out.println("Error Module: " + Arrays.toString(predictionsForTestData.countsForTargets()));
        System.out.println("Error Module: " + predictionsForTestData.getErrorsForTarget("val"));
        System.out.println("Error Module: " + predictionsForTestData.getPredictionsForTarget("val"));
        System.out.println("Error Module: " + predictionsForTestData.getTargetFields());
        for (TSEvalModule tsEval : eval.getEvaluationModules()) {
            List<NumericPrediction> predictionsForTarget = predictionsForTestData.getPredictionsForTarget("val");
            tsEval.setTargetFields(predictionsForTestData.getTargetFields());
            for (int i = 0; i < predictionsForTarget.size(); i++) {
                tsEval.evaluateForInstance(predictionsForTarget.subList(i, i + 1), eval.getTestData().get(i));

            }
            System.out.println(tsEval.getEvalName() + " - " + Arrays.toString(tsEval.calculateMeasure()));
        }
        System.out.println(eval.toSummaryString());
        System.out.println(eval.printPredictionsForTrainingData("Print Predictions for test data", "val", 1));
        System.out.println(eval.printPredictionsForTestData("Print Predictions for test data", "val", 1));
        //Scrivo risultati su file




        // forecaster.getTSLagMaker().setPeriodicity(Periodicity.UNKNOWN);//(Periodicity.DAILY);
        //forecaster.getTSLagMaker().setMinLag(1);
        //forecaster.getTSLagMaker().setMaxLag(dataset.numInstances()); // daily data

        // add a month of the year indicator field
        //forecaster.getTSLagMaker().setAddMonthOfYear(true);
        //forecaster.getTSLagMaker().setAddDayOfMonth(true);
        //forecaster.getTSLagMaker().setAddDayOfWeek(true);
        //forecaster.getTSLagMaker().setAddNumDaysInMonth(true);
        //forecaster.getTSLagMaker().setAddWeekendIndicator(true);

        //forecaster.getTSLagMaker().setAdjustForTrends(true);
        //forecaster.getTSLagMaker().setAdjustForVariance(false);

        // build the model
        //forecaster.setConfidenceLevel(0.95);

        //forecaster.buildForecaster(dataset, System.out);
        //forecaster.primeForecaster(dataset);
        //System.out.println("Numero dati: " + dataset.numInstances() + " user " + uid + " periodicy: " + forecaster.getTSLagMaker().getPeriodicity().name());
        //List<List<NumericPrediction>> forecast = forecaster.forecast(numberOfPrediction, System.out);
        System.exit(1);
    }
    /**
     * Aggiorna i dati della struttura con quelli presenti nella linea
     * @param line 
     */
    private static HashSet<String> txHash = new HashSet<String>();
    private static String prevTx = "";

    public static void predict(Instances dataset) throws Exception {
        LinearRegression function = new LinearRegression();
        //function.setOutputAdditionalStats(true);
        //System.out.println(Arrays.toString(function.getOptions()));
        WekaForecaster forecaster = new WekaForecaster();
        forecaster.setBaseForecaster(function);
        forecaster.getTSLagMaker().setTimeStampField("data"); // date time stamp
        forecaster.setConfidenceLevel(0.95);
        forecaster.setFieldsToForecast("val");
        // forecaster.getTSLagMaker().setDebug(true);
        // forecaster.setConfidenceLevel(0.95);
        System.out.println("Weka Forecaster option:" + Arrays.toString(forecaster.getOptions()));
        System.out.println("Weka LagMaker option:" + Arrays.toString(forecaster.getTSLagMaker().getOptions()));





        TSEvaluation eval = new TSEvaluation(dataset, testSplitSize);
        System.out.println("Timestamp Field: " + forecaster.getTSLagMaker().getTimeStampField());
        System.out.println("Fields To Forecast: " + forecaster.getFieldsToForecast());
        System.out.println("Training set size:" + eval.getTrainingData().size());
        System.out.println("Test set size:" + eval.getTestData().size());
        System.out.println("Build forecastr..");
        //forecaster.buildForecaster(eval.getTrainingData(), System.out);
        //forecaster.primeForecaster(dataset);
        //System.out.println("WF " + forecaster.toString());
        //forecaster.buildForecaster(eval.getTrainingData(), System.out);
        System.out.println("Future forecast: " + eval.getForecastFuture());
        System.out.println("PrimeForTestDataWithTestData: " + eval.getPrimeForTestDataWithTestData());
        System.out.println("PrimeForTestDataWithTestData: " + eval.getPrimeWindowSize());
        eval.setEvaluationModules("MAE,MSE,RMSE,MAPE,DAC,RAE,RRSE");
        //eval.setEvaluateOnTrainingData(true);
        eval.setRebuildModelAfterEachTestForecastStep(false);
        //eval.setEvaluateOnTestData(true);
        //eval.setPrimeForTestDataWithTestData(false);
        //eval.setForecastFuture(true);
        eval.setPrimeWindowSize(12);

        System.out.println("getPrimeForTestDataWithTestData: " + eval.getPrimeForTestDataWithTestData());
        System.out.println("getPrimeWindowSize: " + eval.getPrimeWindowSize());
        System.out.println("getEvaluateOnTestData: " + eval.getEvaluateOnTestData());
        System.out.println("getEvaluateOnTrainingData: " + eval.getEvaluateOnTrainingData());
        System.out.println("getForecastFuture: " + eval.getForecastFuture());
        System.out.println("getAlgorithmName: " + forecaster.getAlgorithmName());
        System.out.println("getFieldsToForecast: " + forecaster.getFieldsToForecast());
        System.out.println("getOverlayFields: " + forecaster.getOverlayFields());
        System.out.println("getCalculateConfIntervalsForForecasts: " + forecaster.getCalculateConfIntervalsForForecasts());
        System.out.println("getConfidenceLevel: " + forecaster.getConfidenceLevel());
        System.out.println("getFieldsToLagAsString: " + forecaster.getTSLagMaker().getFieldsToLagAsString());
        System.out.println("getLagRange: " + forecaster.getTSLagMaker().getLagRange());
        System.out.println("getPrimaryPeriodicFieldName: " + forecaster.getTSLagMaker().getPrimaryPeriodicFieldName());
        System.out.println("getSkipEntries: " + forecaster.getTSLagMaker().getSkipEntries());
        System.out.println("getTimeStampField: " + forecaster.getTSLagMaker().getTimeStampField());
        System.out.println("getAddAMIndicator: " + forecaster.getTSLagMaker().getAddAMIndicator());
        System.out.println("getAddDayOfMonth: " + forecaster.getTSLagMaker().getAddDayOfMonth());
        System.out.println("getAddDayOfWeek: " + forecaster.getTSLagMaker().getAddDayOfWeek());
        System.out.println("getAddMonthOfYear: " + forecaster.getTSLagMaker().getAddMonthOfYear());
        System.out.println("getAddNumDaysInMonth: " + forecaster.getTSLagMaker().getAddNumDaysInMonth());
        System.out.println("getAddQuarterOfYear: " + forecaster.getTSLagMaker().getAddQuarterOfYear());
        System.out.println("getAddWeekendIndicator: " + forecaster.getTSLagMaker().getAddWeekendIndicator());
        System.out.println("getAdjustForTrends: " + forecaster.getTSLagMaker().getAdjustForTrends());
        System.out.println("getAdjustForVariance: " + forecaster.getTSLagMaker().getAdjustForVariance());
        //System.out.println("getArtificialTimeStartValue: " + forecaster.getTSLagMaker().getArtificialTimeStartValue());
        System.out.println("getAverageConsecutiveLongLags: " + forecaster.getTSLagMaker().getAverageConsecutiveLongLags());
        System.out.println("getAverageLagsAfter: " + forecaster.getTSLagMaker().getAverageLagsAfter());
        System.out.println("getDeltaTime: " + forecaster.getTSLagMaker().getDeltaTime());
        System.out.println("getIncludePowersOfTime: " + forecaster.getTSLagMaker().getIncludePowersOfTime());
        System.out.println("getIncludeTimeLagProducts: " + forecaster.getTSLagMaker().getIncludeTimeLagProducts());
        System.out.println("getMaxLag: " + forecaster.getTSLagMaker().getMaxLag());
        System.out.println("getMinLag: " + forecaster.getTSLagMaker().getMinLag());
        System.out.println("getNumConsecutiveLongLagsToAverage: " + forecaster.getTSLagMaker().getNumConsecutiveLongLagsToAverage());
        System.out.println("getPeriodicity: " + forecaster.getTSLagMaker().getPeriodicity().toString());
        System.out.println("getRemoveLeadingInstancesWithUnknownLagValues: " + forecaster.getTSLagMaker().getRemoveLeadingInstancesWithUnknownLagValues());
        eval.evaluateForecaster(forecaster, true, System.out);
        System.out.println("WF " + forecaster.toString());
        ErrorModule predictionsForTestData = eval.getPredictionsForTestData(1);
        System.out.println("Error Module: " + Arrays.toString(predictionsForTestData.calculateMeasure()));
        System.out.println("Error Module: " + Arrays.toString(predictionsForTestData.countsForTargets()));
        System.out.println("Error Module: " + predictionsForTestData.getErrorsForTarget("val"));
        System.out.println("Error Module: " + predictionsForTestData.getPredictionsForTarget("val"));
        System.out.println("Error Module: " + predictionsForTestData.getTargetFields());
        for (TSEvalModule tsEval : eval.getEvaluationModules()) {
            List<NumericPrediction> predictionsForTarget = predictionsForTestData.getPredictionsForTarget("val");
            tsEval.setTargetFields(predictionsForTestData.getTargetFields());
            for (int i = 0; i < predictionsForTarget.size(); i++) {
                tsEval.evaluateForInstance(predictionsForTarget.subList(i, i + 1), eval.getTestData().get(i));

            }
            System.out.println(tsEval.getEvalName() + " - " + Arrays.toString(tsEval.calculateMeasure()));
        }
        System.out.println(eval.toSummaryString());
        System.out.println(eval.printPredictionsForTrainingData("Print Predictions for test data", "val", 1));
        System.out.println(eval.printPredictionsForTestData("Print Predictions for test data", "val", 1));
    }

    /**
     * Aggiungo utente a timeslot
     */
    private static void updateData(String[] line) {
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
     * Legge dal file and inport the transaction from startBlock to finalBlock
     * blockNumber,t.getHash,
    contractAddress,
    GasPrice,
    GasLimit,
    Sender,
    Value,
    "Contract Creation","Contract Call"
    gasUsed
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
     * Load Transactions and their internal calls
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
        String gasUsed = line[5];
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
        inte.setOperation(!callType.isEmpty() ? callType : type);
        inte.setGasUsed(gasUsed);
        inte.setValue(value);
        inte.setSubtraces(subtraces);
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

    /*
     * Carico  dati degl utenti dai grafi
     */
    private static HashMap<Long, DirectedSparseMultigraph<NodeEth, Interaction>> loadGraph(HashMap<String, UserInfo> result, String uid) {
        HashMap<Long, DirectedSparseMultigraph<NodeEth, Interaction>> timeVaryingGrap = new HashMap<Long, DirectedSparseMultigraph<NodeEth, Interaction>>();
        //per ogni file
        txHash.clear();

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


        System.out.println("Load " + result.get(uid) + " " + result.get(uid).startTimeSlot * timeslotSize + " - " + ((result.get(uid).endTimeSlot + 1) * timeslotSize - 1));
        prevTx = "";
        txHash.clear();
        for (File f : files) {
            try {
                String startB = f.getName().substring(f.getName().indexOf('[') + 1, f.getName().indexOf('-'));
                String finalB = f.getName().substring(f.getName().indexOf('-') + 1, f.getName().indexOf(')'));
                System.out.println("File " + f.getName() + " start " + startB + " end " + finalB);
                if (Long.valueOf(finalB) < result.get(uid).startTimeSlot * timeslotSize
                        || Long.valueOf(startB) > (result.get(uid).endTimeSlot + 1) * timeslotSize - 1) {
                    System.out.println("leave.." + f.getName());
                    continue;
                } else {
                    CSVReader csvR = new CSVReader(new FileReader(f), '\t', CSVWriter.DEFAULT_QUOTE_CHARACTER);
                    while ((line = csvR.readNext()) != null) {
                        int blockNumber = Integer.valueOf(line[0]);
                        long timeslotIndex = Math.round(Math.floor((double) blockNumber / (double) MaxTimeslotSize * (double) ntimeslot));

                        if (blockNumber >= result.get(uid).startTimeSlot * timeslotSize
                                && blockNumber <= (result.get(uid).endTimeSlot + 1) * timeslotSize - 1) {
                            //carico il record nel grafo approprito ed esco dal loop
                            if (!timeVaryingGrap.containsKey(timeslotIndex)) {
                                timeVaryingGrap.put(timeslotIndex, new DirectedSparseMultigraph<NodeEth, Interaction>());
                            }
                            if (graphType.equals("G1")) {
                                importG1(line, timeVaryingGrap.get(timeslotIndex));
                            } else if (graphType.equals("G2")) {
                                importG2(line, timeVaryingGrap.get(timeslotIndex));
                            } else if (graphType.equals("G3")) {
                                importG3(line, timeVaryingGrap.get(timeslotIndex));
                            }
                        }
                    }
                    csvR.close();
                }
            } catch (IOException ex) {
                System.out.println("File " + f.getAbsolutePath() + " not found..");
            }
        }
        return timeVaryingGrap;
    }

    /**
     * Create the dataset
     * @param loadGraph
     * @return 
     */
    private static Instances createDataset(HashMap<Long, DirectedSparseMultigraph<NodeEth, Interaction>> loadGraph, HashMap<Long, Double> prepareValToPredict, String uid) {
        //Creo dataset 
        ArrayList<Attribute> attInfo = new ArrayList<Attribute>();
        Attribute timestamp = new Attribute("data", "dd/MM/yyyy");

        Attribute centrality = new Attribute("val");
        attInfo.add(timestamp);
        attInfo.add(centrality);
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
            for (long ts : loadGraph.keySet()) {
                Instance record = new DenseInstance(2);
                record.setValue(timestamp, Long.valueOf(t.get(ts * timeslotSize)));
                record.setValue(centrality, prepareValToPredict.get(ts));
                dataset.add(record);
            }
            t.clear();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return dataset;
    }

    /**
     * Calcola i valori dal grafo
     * @param loadGraph
     * @param uid
     */
    private static HashMap<Long, Double> prepareValToPredict(HashMap<Long, DirectedSparseMultigraph<NodeEth, Interaction>> loadGraph, String uid, int prop) {
        HashMap<Long, Double> res = new HashMap<Long, Double>();
        for (long ts : loadGraph.keySet()) {
            switch (prop) {
                case EDGE_IN_DEGREE:
                    res.put(ts, (double) loadGraph.get(ts).getInEdges(nodesMap.get(uid)).size());
                    break;
                case EDGE_OUT_DEGREE:
                    res.put(ts, (double) loadGraph.get(ts).getOutEdges(nodesMap.get(uid)).size());
                    break;
                case VERTEX_IN_DEGREE:
                    HashSet<String> inDeg = new HashSet<String>();
                    for (Interaction e : loadGraph.get(ts).getInEdges(nodesMap.get(uid))) {
                        NodeEth source = loadGraph.get(ts).getSource(e);
                        inDeg.add(source.address);
                    }
                    res.put(ts, (double) inDeg.size());
                    break;
                case VERTEX_OUT_DEGREE:
                    HashSet<String> outDeg = new HashSet<String>();
                    for (Interaction e : loadGraph.get(ts).getOutEdges(nodesMap.get(uid))) {
                        NodeEth dest = loadGraph.get(ts).getDest(e);
                        outDeg.add(dest.address);
                    }
                    res.put(ts, (double) outDeg.size());
                    break;
            }
        }
        return res;
    }
}

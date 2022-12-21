/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package prediction;

import com.opencsv.CSVWriter;
import dataModel.graphModel.NodeEth;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import weka.classifiers.evaluation.NumericPrediction;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.timeseries.WekaForecaster;
import weka.classifiers.timeseries.eval.ErrorModule;
import weka.classifiers.timeseries.eval.TSEvalModule;
import weka.classifiers.timeseries.eval.TSEvaluation;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

/**
 * Prediction on blockchain timeseries. Read data from db
 * @author andreads
 */
public class WekaTimeseriesEthPredictorData {

    /**
     * TRAINING
     */
    //  Trainig/Test set split fraction
    static final double testSplitSize = 0.1;
    /** Numero di blocchi in un intervallo */
    static int timeslotSize = 50000;
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
    /** Directory in cui si trovano i dati */
    static String dataDirName = "/home/andreads/NetBeansProjects/EthereumTesting/dataCorrectBesu/";
    /** Type of graph graphType(G1,G2,G3)*/
    static String graphType = "G1";
    //Smart Contract Temporal Networks
    private static HashMap<String, NodeEth> nodesMap = new HashMap<String, NodeEth>();
    //Fields to forecast
    private static String[] fieldsToForecast = new String[]{EDGE_IN_DEGREE,
        EDGE_OUT_DEGREE, VERTEX_IN_DEGREE, VERTEX_OUT_DEGREE, ETH_IN_EOA, ETH_IN_SC, ETH_OUT_EOA, ETH_OUT_SC};

    public static void main(String[] args) throws Exception {
        initiCLIoption(args);


        System.out.println("Data dir: " + dataDirName);
        //System.out.println("Grapht Type: " + graphType);
        //System.out.println("Timestamp File: " + timestampTimeFile);

        if (dataDirName.contains(File.pathSeparator + "G1" + File.pathSeparator)) {
            graphType = "G1";
        } else if (dataDirName.contains(File.pathSeparator + "G2" + File.pathSeparator)) {
            graphType = "G2";
        } else if (dataDirName.contains(File.pathSeparator + "G3" + File.pathSeparator)) {
            graphType = "G3";
        }

        //--------------------------
        //READ USERS FOR PREDICTION
        //--------------------------
        //HashMap<String, UserInfo> result = new HashMap<String, UserInfo>();
        //Numero timeslot consecutivi
        //result = readUsersForPrediction();


        //Per ogni utente creo un record

        //LEGGO tutti i file
        File dataDir = new File(dataDirName);
        if (!(dataDir.isDirectory())) {
            System.out.println("File " + dataDirName + " is not a dir..");
            System.exit(1);
        }

        String[] line;

        File[] files = dataDir.listFiles();

        String fileOutputName = "Result" + graphType + "Delta" + timeslotSize + ".csv";
        CSVWriter w = new CSVWriter(new FileWriter(fileOutputName), ',', CSVWriter.NO_QUOTE_CHARACTER);
        w.writeNext(new String[]{"TARGET", "isSC", "Error", "MAE", "MSE", "RMSE", "MAPE", "DAC", "RAE", "RRSE"});

        for (File f : files) {
            try {
                System.out.println("Load file " + f.getName());
                ArffLoader loader = new ArffLoader();
                loader.setFile(f);
                Instances dataset = loader.getDataSet();
                for (String target : fieldsToForecast) {
                    LinearRegression function = new LinearRegression();

                    //function.setOutputAdditionalStats(true);
                    //System.out.println(Arrays.toString(function.getOptions()));
                    WekaForecaster forecaster = new WekaForecaster();
                    forecaster.setConfidenceLevel(0.95);
                    forecaster.setFieldsToForecast(target);
                    //forecaster.getTSLagMaker().setDebug(true);


                    forecaster.setBaseForecaster(function);
                    forecaster.getTSLagMaker().setTimeStampField("data"); // date time stamp
                    System.out.println("Weka Forecaster:" + Arrays.toString(forecaster.getOptions()));


                    TSEvaluation eval = new TSEvaluation(dataset, testSplitSize);
                    System.out.println("Timestamp Field: " + forecaster.getTSLagMaker().getTimeStampField());
                    System.out.println("Fields To Forecast: " + forecaster.getFieldsToForecast());
                    System.out.println("Training set size:" + eval.getTrainingData().size());
                    System.out.println("Test set size:" + eval.getTestData().size());
                    System.out.println("Build forecastr..");
                    //forecaster.buildForecaster(eval.getTrainingData(), System.out);
                    //System.out.println("WF " + forecaster.toString());
                    //forecaster.buildForecaster(eval.getTrainingData(), System.out);
                    //System.out.println("Future forecast: " + eval.getForecastFuture());
                    //System.out.println("PrimeForTestDataWithTestData: " + eval.getPrimeForTestDataWithTestData());
                    //System.out.println("PrimeForTestDataWithTestData: " + eval.getPrimeWindowSize());
                    eval.setEvaluationModules("MAE,MSE,RMSE,MAPE,DAC,RAE,RRSE");
                    //eval.setEvaluateOnTrainingData(true);
                    eval.setRebuildModelAfterEachTestForecastStep(false);
                    //eval.setEvaluateOnTestData(true);
                    //eval.setPrimeForTestDataWithTestData(true);
                    eval.setPrimeWindowSize(forecaster.getTSLagMaker().getMaxLag());
//                System.out.println("getPrimeForTestDataWithTestData: " + eval.getPrimeForTestDataWithTestData());
//                System.out.println("getPrimeWindowSize: " + eval.getPrimeWindowSize());
//                System.out.println("getEvaluateOnTestData: " + eval.getEvaluateOnTestData());
//                System.out.println("getEvaluateOnTrainingData: " + eval.getEvaluateOnTrainingData());
//                System.out.println("getForecastFuture: " + eval.getForecastFuture());
//                System.out.println("getAlgorithmName: " + forecaster.getAlgorithmName());
//                System.out.println("getFieldsToForecast: " + forecaster.getFieldsToForecast());
//                System.out.println("getOverlayFields: " + forecaster.getOverlayFields());
//                System.out.println("getCalculateConfIntervalsForForecasts: " + forecaster.getCalculateConfIntervalsForForecasts());
//                System.out.println("getConfidenceLevel: " + forecaster.getConfidenceLevel());
//                System.out.println("getFieldsToLagAsString: " + forecaster.getTSLagMaker().getFieldsToLagAsString());
//                System.out.println("getLagRange: " + forecaster.getTSLagMaker().getLagRange());
//                System.out.println("getPrimaryPeriodicFieldName: " + forecaster.getTSLagMaker().getPrimaryPeriodicFieldName());
//                System.out.println("getSkipEntries: " + forecaster.getTSLagMaker().getSkipEntries());
//                System.out.println("getTimeStampField: " + forecaster.getTSLagMaker().getTimeStampField());
//                System.out.println("getAddAMIndicator: " + forecaster.getTSLagMaker().getAddAMIndicator());
//                System.out.println("getAddDayOfMonth: " + forecaster.getTSLagMaker().getAddDayOfMonth());
//                System.out.println("getAddDayOfWeek: " + forecaster.getTSLagMaker().getAddDayOfWeek());
//                System.out.println("getAddMonthOfYear: " + forecaster.getTSLagMaker().getAddMonthOfYear());
//                System.out.println("getAddNumDaysInMonth: " + forecaster.getTSLagMaker().getAddNumDaysInMonth());
//                System.out.println("getAddQuarterOfYear: " + forecaster.getTSLagMaker().getAddQuarterOfYear());
//                System.out.println("getAddWeekendIndicator: " + forecaster.getTSLagMaker().getAddWeekendIndicator());
//                System.out.println("getAdjustForTrends: " + forecaster.getTSLagMaker().getAdjustForTrends());
//                System.out.println("getAdjustForVariance: " + forecaster.getTSLagMaker().getAdjustForVariance());
//                System.out.println("getAverageConsecutiveLongLags: " + forecaster.getTSLagMaker().getAverageConsecutiveLongLags());
//                System.out.println("getAverageLagsAfter: " + forecaster.getTSLagMaker().getAverageLagsAfter());
//                System.out.println("getDeltaTime: " + forecaster.getTSLagMaker().getDeltaTime());
//                System.out.println("getIncludePowersOfTime: " + forecaster.getTSLagMaker().getIncludePowersOfTime());
//                System.out.println("getIncludeTimeLagProducts: " + forecaster.getTSLagMaker().getIncludeTimeLagProducts());
//                System.out.println("getMaxLag: " + forecaster.getTSLagMaker().getMaxLag());
//                System.out.println("getMinLag: " + forecaster.getTSLagMaker().getMinLag());
//                System.out.println("getNumConsecutiveLongLagsToAverage: " + forecaster.getTSLagMaker().getNumConsecutiveLongLagsToAverage());
//                System.out.println("getPeriodicity: " + forecaster.getTSLagMaker().getPeriodicity().toString());
//                System.out.println("getRemoveLeadingInstancesWithUnknownLagValues: " + forecaster.getTSLagMaker().getRemoveLeadingInstancesWithUnknownLagValues());
                    eval.evaluateForecaster(forecaster, true, System.out);
                    //System.out.println("WF " + forecaster.toString());
                    ErrorModule predictionsForTestData = eval.getPredictionsForTestData(1);
                    //System.out.println("Error Module: " + Arrays.toString(predictionsForTestData.calculateMeasure()));
                    //System.out.println("Error Module: " + Arrays.toString(predictionsForTestData.countsForTargets()));
                    //System.out.println("Error Module: " + predictionsForTestData.getErrorsForTarget(target));
                    //System.out.println("Error Module: " + predictionsForTestData.getPredictionsForTarget(target));
                    //System.out.println("Error Module: " + predictionsForTestData.getTargetFields());
                    List<NumericPrediction> predictionsForTarget = predictionsForTestData.getPredictionsForTarget(target);
                    String[] errors = new String[10];
                    errors[0] = target;
                    errors[1] = String.valueOf(f.getName().endsWith("-SC"));
                    int index = 2;
                    for (TSEvalModule tsEval : eval.getEvaluationModules()) {
                        tsEval.setTargetFields(Arrays.asList(new String[]{target}));
                        for (int i = 0; i < predictionsForTarget.size(); i++) {
                            //System.out.println("Evaluate " + predictionsForTarget.subList(i, i + 1).toString() + " - " + eval.getTestData().get(i).toString());
                            tsEval.evaluateForInstance(predictionsForTarget.subList(i, i + 1), eval.getTestData().get(i));
                        }
                        System.out.println(tsEval.getEvalName() + " - " + Arrays.toString(tsEval.calculateMeasure()));
                        errors[index] = String.valueOf(tsEval.calculateMeasure()[0]);
                        index++;
                    }
                    w.writeNext(errors);
                    //System.out.println(eval.toSummaryString());
                    //System.out.println(eval.printPredictionsForTrainingData("Print Predictions for test data", target, 1));
                    //System.out.println(eval.printPredictionsForTestData("Print Predictions for test data", target, 1));
                }
            } catch (IOException ex) {
                System.out.println("File " + f.getAbsolutePath() + " not found..");
            }
        }
        w.close();
    }

    /**
     * Command line option
     */
    private static void initiCLIoption(String[] args) {

        Option timeslotSizeOption = Option.builder("timeslotSize").argName("value").required().hasArg().desc("Number of consecutive blocks in each time slot").build();
        Option dataDirNameOption = Option.builder("dataDirName").argName("path").hasArg().required().desc("Directory of the dataset").build();
        Option help = new Option("help", "print this message");
        // create the parser
        Options options = new Options();
        options.addOption(help);
        options.addOption(dataDirNameOption);
        options.addOption(timeslotSizeOption);
        CommandLineParser parser = new DefaultParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);


            if (line.hasOption("dataDirName")) {
                // initialise the member variable
                dataDirName = line.getOptionValue("dataDirName");
            }

            if (line.hasOption("timeslotSize")) {
                // initialise the member variable
                timeslotSize = Integer.valueOf(line.getOptionValue("timeslotSize"));
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
}

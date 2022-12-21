/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package statistics;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import org.apache.commons.math3.stat.Frequency;

/**
 *
 * @author andreads
 */
public class Test {

    public static void main(String[] args) {
        try {
            Frequency freIn = new Frequency();
            Frequency freOut = new Frequency();
            CSVReader r = new CSVReader(new FileReader("/home/andreads/NetBeansProjects/WekaEthPrediction/graphStatistics/GraphStatisticsG1Delta50000.csv"), ',', CSVWriter.NO_QUOTE_CHARACTER);
            String[] line;
            //r.readNext();
            while ((line = r.readNext()) != null) {
                freIn.addValue(Double.valueOf(line[3]));
                freOut.addValue(Double.valueOf(line[4]));
            }
            r.close();
            System.out.println("Total count: " + freIn.getSumFreq() + " - " + freOut.getSumFreq());
            System.out.println("InDegree 0.0: " + freIn.getCount(0.0) + " valori - " + freIn.getPct(0.0) + " - " + freIn.getCumFreq(0.0) + " - " + freIn.getCumPct(0.0));
            System.out.println("InDegree 1.0: " + freIn.getCount(1.0) + " valori - " + freIn.getPct(1.0) + " - " + freIn.getCumFreq(1.0) + " - " + freIn.getCumPct(1.0));

            System.out.println("OutDegree 0: " + freOut.getCount(0) + " valori - " + freOut.getPct(0) + " - " + freOut.getCumFreq(0) + " - " + freOut.getCumPct(0));
            System.out.println("OutDegree 1: " + freOut.getCount(1) + " valori - " + freOut.getPct(1) + " - " + freOut.getCumFreq(1) + " - " + freOut.getCumPct(1));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package statistics;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Calcolo la distribuzione dei timeslot in cui l'utente e' attivo
 * @author andreads
 */
public class AddressTimeDistribution {

    /** Numero di blocchi in un intervallo */
    static int timeslotSize = 200000;
    /** Directory in cui si trovano i dati */
    static String dataDirName = "/home/andreads/NetBeansProjects/EthereumTesting/dataCorrectBesu/";
    /** Type of graph graphType(G1,G2,G3)*/
    static String graphType = "G1";
    //Timeslot -> UID Presenti -> true if UID=SC false if UID=EOA
    private static HashMap<Long, HashMap<String, Boolean>> usersTimeslot = new HashMap<>();
    /** Numero Max di blocchi del dataset */
    static final int MaxTimeslotSize = 4000000;
    /** Numero di timeslot */
    static int ntimeslot;

    public static void main(String[] args) {
        try {
            //Leggo dati input 

            timeslotSize = Integer.valueOf(args[0]);
            graphType = args[1];
            dataDirName = args[2];
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
            for (File f : files) {
                try {
                    CSVReader csvR = new CSVReader(new FileReader(f), '\t', CSVWriter.DEFAULT_QUOTE_CHARACTER);
                    while ((line = csvR.readNext()) != null) {
                        updateData(line);
                    }
                    System.out.println(usersTimeslot.keySet());
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



            HashMap<Integer, Integer[]> result = new HashMap<Integer, Integer[]>();
            //Numero timeslot consecutivi
            for (int n = 1; n <= ntimeslot; n++) {
                System.out.println("Numero time slot: " + n);
                result.put(n, new Integer[]{0, 0, 0});
                //Calcolo il numero di utenti che hanno n timeslot consecutivi   
                for (String uid : usersActiveSlot.keySet()) {
                    ArrayList<Long> get = usersActiveSlot.get(uid);
                    if (get.size() < n) {
                        continue;
                    } else if (get.size() >= n && n == 1) {
                        //utente uid e' attivo almeno n timeslot
                        result.get(n)[0]++;
                        if (usersTimeslot.get(get.get(0)).get(uid)) {
                            result.get(n)[2]++;
                        } else {
                            result.get(n)[1]++;
                        }
                    } else {
                        Collections.sort(get);
                        int contaConsecutivi = 1;
                        for (int i = 1; i < get.size(); i++) {
                            if (get.get(i) == get.get(i - 1) + 1) {
                                contaConsecutivi++;
                            } else {
                                contaConsecutivi = 1;
                            }

                            if (contaConsecutivi == n) {
                                result.get(n)[0]++;
                                if (usersTimeslot.get(get.get(0)).get(uid)) {
                                    result.get(n)[2]++;
                                } else {
                                    result.get(n)[1]++;
                                }
                                break;
                            }
                        }
                    }

                }
                //TEST


            }



            //Output
            CSVWriter w = new CSVWriter(new FileWriter("UserTimeSlotDistribution.csv"), ',', CSVWriter.NO_QUOTE_CHARACTER);
            w.writeNext(new String[]{"#TimeSlotConsecutivi", "#Tot", "#EOA", "#SC"});
            for (int consecutiveTimeslot : result.keySet()) {
                w.writeNext(new String[]{String.valueOf(consecutiveTimeslot),
                            String.valueOf(result.get(consecutiveTimeslot)[0]),
                            String.valueOf(result.get(consecutiveTimeslot)[1]),
                            String.valueOf(result.get(consecutiveTimeslot)[2])
                        });
            }
            w.close();
            System.exit(1);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    /**
     * Aggiorna i dati della struttura con quelli presenti nella linea
     * @param line 
     */
    private static HashSet<String> txHash = new HashSet<String>();
    private static String prevTx = "";

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
}

package vassar.combinatorics;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import evaluator.EvaluatorApp;
import org.paukov.combinatorics3.IGenerator;
import vassar.architecture.SingleSat;
import vassar.evaluator.ADDEvaluator;
import vassar.jess.Resource;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import org.paukov.combinatorics3.Generator;
import vassar.result.Result;

public class Combinatorics {



    // Computes synergy NDSM for instruments (for all orbits)
    // SDSN: synergy based
    // EDSM: cost based
    public static HashMap<String,NDSM> computeNDSM(Resource res, String[] instruments, int dim){
        HashMap<String,NDSM> dsm_map = new HashMap<>();
        IGenerator gen = Generator.combination(instruments).simple(dim-1);

        // Iterate over orbits
        for(int x = 0; x < res.problem.getNumOrbits(); x++){
            String orbit = res.problem.orbitList[x];
            String key_r = "RDSM" + dim + "@" + orbit;
            String key_s = "SDSM" + dim + "@" + orbit;
            String key_e = "EDSM" + dim + "@" + orbit;



            // Iterate over combinations
            Iterator it = gen.iterator();
            while(it.hasNext()){
                ArrayList<String> combination = (ArrayList<String>)it.next();
                System.out.println(combination);
                // EvaluatorApp.sleep(3);

                // Iterate over number of instruments
                for(int j = 0; j < res.problem.getNumInstr(); j++){
                    String instrument = res.problem.instrumentList[j];

                    if(combination.contains(instrument)){
                        continue;
                    }
                    // combination --> base
                    // instrumenmt --> added
                    Nto1pair nto10 = new Nto1pair(combination, instrument);

                    // 1. Create Architecture
//                    System.out.println("---> NDSM ARCH TO BE EVALUATED");
//                    System.out.println(orbit);
//                    System.out.println(combination);
//                    System.out.println(instrument);
//                    EvaluatorApp.sleep(5);

                    ArrayList<String> appended_insts = new ArrayList<>(combination);
                    appended_insts.add(instrument);
                    SingleSat arch = new SingleSat(appended_insts, orbit);

                    // 2. Evaluate Architecture with Synergies
                    ADDEvaluator process1 = new ADDEvaluator.Builder(res)
                            .setArchitecture(arch)
                            .evalCost(true)
                            .evalPerformance(true)
                            .evalScheduling(true)
                            .orbitSelection(false)
                            .synergyDeclaration(true)
                            .build();
                    ExecutorService executorService1 = Executors.newFixedThreadPool(1);
                    Future<Result> future1 = executorService1.submit(process1);
                    Result result1 = null;
                    try {
                        result1 = future1.get();
                    }
                    catch (ExecutionException e) {
                        System.out.println("Exception when evaluating an architecture");
                        e.printStackTrace();
                        System.exit(-1);
                    }
                    catch (InterruptedException e) {
                        System.out.println("Execution got interrupted while evaluating an architecture");
                        e.printStackTrace();
                        System.exit(-1);
                    }

                    // 3. Evaluate Architecture without Synergies
                    ADDEvaluator process2 = new ADDEvaluator.Builder(res)
                            .setArchitecture(arch)
                            .evalCost(true)
                            .evalPerformance(true)
                            .evalScheduling(true)
                            .orbitSelection(false)
                            .synergyDeclaration(false)
                            .build();
                    ExecutorService executorService2 = Executors.newFixedThreadPool(1);
                    Future<Result> future2 = executorService2.submit(process2);
                    Result result2 = null;
                    try {
                        result2 = future2.get();
                    }
                    catch (ExecutionException e) {
                        System.out.println("Exception when evaluating an architecture");
                        e.printStackTrace();
                        System.exit(-1);
                    }
                    catch (InterruptedException e) {
                        System.out.println("Execution got interrupted while evaluating an architecture");
                        e.printStackTrace();
                        System.exit(-1);
                    }


                    // CREATE NDSM

                    // Get score differences
                    double red = result1.getScience() - result2.getScience();
                    double cst = result1.getCost() - result2.getCost();

                    System.out.println("---> SCIENCE DIFF: " + red);
                    System.out.println("---> COST DIFF: " + cst);
                    // EvaluatorApp.sleep(5);

                    // Index Synergy Based: SDSM
                    NDSM sdsm = dsm_map.get(key_s);
                    if(sdsm == null){
                        sdsm = new NDSM(res.problem.instrumentList, key_s);
                        dsm_map.put(key_s, sdsm);
                    }
                    sdsm.setInteraction(nto10.getBase(),nto10.getAdded(), red);

                    // Index Cost Based: EDSM
                    NDSM edsm = dsm_map.get(key_e);
                    if(edsm == null){
                        edsm = new NDSM(res.problem.instrumentList, key_e);
                        dsm_map.put(key_e, edsm);
                    }
                    edsm.setInteraction(nto10.getBase(),nto10.getAdded(), cst);
                }
            }
        }

        try{
            SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd-HH-mm-ss" );
            String stamp = dateFormat.format( new Date() );
            FileOutputStream file = new FileOutputStream( "/app/output/DSM-Decadal"+dim+"-" + stamp + ".dat");
            ObjectOutputStream os = new ObjectOutputStream( file );
            os.writeObject( dsm_map );
            os.close();
            file.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }

        return dsm_map;
    }




    public static TreeMap<Nto1pair,Double> combineNDSM_File(String file, String type){
        HashMap<String, NDSM> dinem_2 = Combinatorics.getNDSMs(file);

        ArrayList<NDSM> ndsms = new ArrayList<>();
        for(String key: dinem_2.keySet()){
            if(!key.contains(type)){
                continue;
            }
            ndsms.add(dinem_2.get(key));
        }

        return Combinatorics.combineNDSMs(ndsms);
    }

    public static HashMap<String,NDSM> getNDSMs(String path){
        HashMap<String,NDSM> result = null;

        try{
            FileInputStream stream = new FileInputStream(path);
            ObjectInputStream object_stream = new ObjectInputStream(stream);
            result = (HashMap) object_stream.readObject();
            return result;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    public static TreeMap<Nto1pair,Double> combineNDSMs(ArrayList<NDSM> ndsms){
        System.out.println("--> SIZE: " + ndsms.size());

        HashMap<Nto1pair,Double> combined_map = new HashMap<>();

        for(NDSM dsm: ndsms){
            // Get MAP
            HashMap<Nto1pair,Double> map = dsm.getMap();
            for(Nto1pair key: map.keySet()){
                if(combined_map.containsKey(key)){
                    Double new_val = combined_map.get(key) + map.get(key);
                    combined_map.put(key, new_val);
                }
                else{
                    combined_map.put(key, map.get(key));
                }
            }
        }

        TreeMap<Nto1pair,Double> interactions = Combinatorics.getNDSM_Interactions("+", combined_map);
        System.out.println("--> INTERACTIONS: " + interactions);
        return interactions;
    }






    public static void readNDSM_File(String path, int print_wait){

        try {

            // Get ndsm_map: orbit --> NDSM
            FileInputStream stream = new FileInputStream(path);
            ObjectInputStream object_stream = new ObjectInputStream(stream);
            HashMap<String,NDSM> ndsm_map = (HashMap) object_stream.readObject();
            object_stream.close();


            for(String key: ndsm_map.keySet()){
                System.out.println("\n--------- NDSM ---------");
                NDSM sdsm = ndsm_map.get(key);
                System.out.println(key);
                System.out.println(sdsm.getIndices());

                TreeMap<Nto1pair,Double> tm = sdsm.getAllInteractions("+");
                System.out.println(tm);

                for(Nto1pair nt: tm.keySet()){

                    ArrayList<String> al = new ArrayList<>();
                    Collections.addAll(al,nt.getBase());
                    al.add(nt.getAdded());
                    System.out.println("---> al: " + al + " " + tm);


                }

                System.out.println("------------------------");


            }

        }
        catch (Exception e){
            e.printStackTrace();
        }


        // EvaluatorApp.sleep(print_wait);
    }










    public static TreeMap<Nto1pair,Double> getNDSM_Interactions(String operator, HashMap<Nto1pair,Double> map) {
        HashMap<Nto1pair,Double> unsorted_map = new HashMap<Nto1pair,Double>();
        ValueComparator2 bvc =  new ValueComparator2(map);
        TreeMap<Nto1pair,Double> sorted_map = new TreeMap<Nto1pair,Double>(bvc);

        for (Nto1pair key : map.keySet()) {
            Double val = map.get(key);
            if ((val==0.0 && operator.equalsIgnoreCase("0")) || (val>0.0 && operator.equalsIgnoreCase("+")) || (val<0.0 && operator.equalsIgnoreCase("-"))) {
                unsorted_map.put(key,val);
            }
        }
        sorted_map.putAll(unsorted_map);
        return sorted_map;
    }









}

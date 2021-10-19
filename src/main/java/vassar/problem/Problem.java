package vassar.problem;

import com.evaluator.*;
import com.google.gson.JsonObject;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.extension.AbstractExtension;
import evaluator.ResourcePaths;
import vassar.database.DatabaseClient;
import vassar.database.template.TemplateRequest;
import vassar.database.template.request.CapabilityRuleTemplateRequest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// BASE PARAMS EQUIVALENT
public class Problem {

    // -- AGGREGATION --
    public ArrayList<Double>       panelWeights;
    public ArrayList<String>       panelNames;
    public HashMap<String, Double> panelWeightMap;
    public HashMap<String, String> panelDescriptions;
    public ArrayList<Integer>      numObjectivesPerPanel;

    public ArrayList<ArrayList<Double>> objWeights;
    public ArrayList<ArrayList<String>> objNames;
    public HashMap<String, String>      objectiveDescriptions;
    public ArrayList<ArrayList<ArrayList<String>>> subobjectives;
    public HashMap<String, JsonObject>             subobjectiveDetails;

    public ArrayList<ArrayList<ArrayList<Double>>> subobjWeights;
    public HashMap<String, String> subobjDescriptions;
    public HashMap<String, Double> subobjWeightsMap;



    public int MAX_TOTAL_INSTR;
    public int numInstr;
    public int numOrbits;
    public int numPanels;
    public String requestMode;
    public String orekitResourcesPath;
    public String[] orbitList;
    public String[] instrumentList;
    public ArrayList<String> parameterList;
    protected HashMap<String, Integer> instrumentIndexes;
    protected HashMap<String, Integer> orbitIndexes;
    public HashMap<String, HashMap<String, Double>> revtimes;
    public HashMap<String, String>            subobjectivesToMeasurements;
    public HashMap<String, ArrayList<String>> measurementsToSubobjectives;
    public HashMap<String, ArrayList<String>> instrumentsToMeasurements;
    public HashMap<String, ArrayList<String>> measurementsToInstruments;

    public static class Builder<T extends Builder<T>>{


        public ArrayList<String> parameterList;


        public String[] orbitList;
        public String[] instrumentList;
        public String   requestMode;
        public int      numPanels;

        public HashMap<String, String>            subobjectivesToMeasurements;
        public HashMap<String, ArrayList<String>> measurementsToSubobjectives;
        public HashMap<String, ArrayList<String>> instrumentsToMeasurements;
        public HashMap<String, ArrayList<String>> measurementsToInstruments;
        // public HashMap<String, ArrayList<String>> objectivesToInstruments;

        // -- AGGREGATION --
        public ArrayList<Double>       panelWeights;
        public ArrayList<String>       panelNames;
        public HashMap<String, Double> panelWeightMap;
        public HashMap<String, String> panelDescriptions;
        public ArrayList<Integer>      numObjectivesPerPanel;

        public ArrayList<ArrayList<Double>> objWeights;
        public ArrayList<ArrayList<String>> objNames;
        public HashMap<String, String>      objectiveDescriptions;
        public ArrayList<ArrayList<ArrayList<String>>> subobjectives;

        public ArrayList<ArrayList<ArrayList<Double>>> subobjWeights;
        public HashMap<String, String> subobjDescriptions;
        public HashMap<String, Double> subobjWeightsMap;


        public Builder() {
            this.parameterList = new ArrayList<>();
            this.instrumentList = new String[] {};
            this.orbitList      = new String[] {};

            subobjectivesToMeasurements = new HashMap<>();
            measurementsToSubobjectives = new HashMap<>();
            instrumentsToMeasurements = new HashMap<>();
            measurementsToInstruments = new HashMap<>();
        }

        public T setInstrumentList(List<EnabledInstrumentsQuery.Item> items){
            this.instrumentList = new String[items.size()];
            int counter = 0;

            System.out.println("\n\n--------------- PROBLEM INSTRUMENTS ---------------");
            for (EnabledInstrumentsQuery.Item item: items){
                System.out.println(item.instrument().name());
                this.instrumentList[counter] = item.instrument().name();
                counter ++;
            }
            System.out.println("---------------------------------------------------\n");

            // this.instrumentList = new String[]{"BIOMASS", "SMAP_RAD", "SMAP_MWR", "CMIS", "VIIRS"};
            return (T) this;
        }

        public T setOrbitList(List<ProblemOrbitJoinQuery.Item> items){
            this.orbitList = new String[items.size()];
            int counter = 0;

            System.out.println("\n\n--------------- PROBLEM ORBITS ---------------");
            for (ProblemOrbitJoinQuery.Item item: items){
                System.out.println(item.Orbit().name());
                this.orbitList[counter] = item.Orbit().name();
                counter ++;
            }
            System.out.println("----------------------------------------------\n");

            // this.orbitList = new String[]{"LEO-600-polar-NA", "SSO-600-SSO-AM", "SSO-600-SSO-DD", "SSO-800-SSO-AM", "SSO-800-SSO-DD"};
            return (T) this;
        }


        public T setInstrumentMeasurementData(ArrayList<CapabilityRuleTemplateRequest.CapabilityRules.Instrument> instruments){
            for(CapabilityRuleTemplateRequest.CapabilityRules.Instrument instrument : instruments) {
                ArrayList<String> measurement_list = new ArrayList<>();
                for(CapabilityRuleTemplateRequest.CapabilityRules.Measurement measurement : instrument.measurements) {
                    measurement_list.add('"'+measurement.name+'"');
                }
                instrumentsToMeasurements.put(instrument.name, measurement_list);
            }
            measurementsToInstruments = this.getInverseHashMapSALToSAL(instrumentsToMeasurements);
            return (T) this;
        }

        private HashMap<String, ArrayList<String>> getInverseHashMapSALToSAL(HashMap<String, ArrayList<String>> hm) {
            HashMap<String, ArrayList<String>> inverse = new HashMap<>();
            for (Map.Entry<String, ArrayList<String>> entr: hm.entrySet()) {
                String key = entr.getKey();
                ArrayList<String> vals = entr.getValue();
                for (String val: vals) {
                    if (inverse.containsKey(val)) {
                        ArrayList<String> list = inverse.get(val);
                        if (!list.contains(key)) {
                            list.add(key);
                            inverse.put(val, list);
                        }
                    }
                    else {
                        ArrayList<String> list = new ArrayList<>();
                        list.add(key);
                        inverse.put(val, list);
                    }
                }
            }
            return inverse;
        }

        public T setAggregationRules(List<AggregationRuleQuery.Item> rules){

            // PANELS
            this.panelWeights          = new ArrayList<>();
            this.panelWeightMap        = new HashMap<>();
            this.panelNames            = new ArrayList<>();
            this.numObjectivesPerPanel = new ArrayList<>();
            this.panelDescriptions     = new HashMap<>();
            this.numPanels             = rules.size();

            // OBJECTIVES
            this.objWeights            = new ArrayList<>();
            this.objNames              = new ArrayList<>();
            this.objectiveDescriptions = new HashMap<>();

            // SUBOVJECTIVES
            this.subobjWeights         = new ArrayList<>();
            this.subobjectives         = new ArrayList<>();
            this.subobjDescriptions    = new HashMap<>();
            this.subobjWeightsMap      = new HashMap<>();

            for (AggregationRuleQuery.Item panel : rules) {  // FOR EACH: panel
                this.panelWeights.add(Double.parseDouble(panel.weight().toString()));
                this.panelNames.add(panel.name());
                this.panelWeightMap.put(panel.name(), Double.parseDouble(panel.weight().toString()));
                this.numObjectivesPerPanel.add(panel.objectives().size());
                this.panelDescriptions.put(panel.name(), panel.description());

                ArrayList<ArrayList<Double>> subobjWeights2 = new ArrayList<>();
                ArrayList<ArrayList<String>> subobjectives2 = new ArrayList<>();
                ArrayList<Double> objWeights2 = new ArrayList<>();
                ArrayList<String> objNames2   = new ArrayList<>();
                for (AggregationRuleQuery.Objective objective : panel.objectives()){  // FOR EACH: objective
                    objWeights2.add(Double.parseDouble(objective.weight().toString()));
                    objNames2.add(objective.name());
                    this.objectiveDescriptions.put(objective.name(), objective.description());

                    ArrayList<Double> subobjWeights3 = new ArrayList<>();
                    ArrayList<String> subobjectives3 = new ArrayList<>();
                    for (AggregationRuleQuery.Subobjective subobjective : objective.subobjectives()){ // FOR EACH: subobjective
                        this.subobjDescriptions.put(subobjective.name(), subobjective.description());
                        this.subobjWeightsMap.put(subobjective.name(), Double.parseDouble(subobjective.weight().toString()));
                        subobjWeights3.add(((BigDecimal) subobjective.weight()).doubleValue());
                        subobjectives3.add(subobjective.name());
                    }
                    subobjWeights2.add(subobjWeights3);
                    subobjectives2.add(subobjectives3);
                }
                subobjWeights.add(subobjWeights2);
                subobjectives.add(subobjectives2);
                this.objWeights.add(objWeights2);
                this.objNames.add(objNames2);
            }
            return (T) this;
        }

        public T setRequestMode(String requestMode){
            this.requestMode = requestMode;
//            System.out.println("REQUEST MODE!!! " + this.requestMode);
//            try                            { TimeUnit.SECONDS.sleep(10); }
//            catch (InterruptedException e) { e.printStackTrace(); }
            return (T) this;
        }

        public T template() {
            return (T) this;
        }

        public Problem build(DatabaseClient client) { return new Problem(this, client);}
    }

    protected Problem(Builder<?> builder, DatabaseClient client){

        // -- AGGREGATION --
        this.panelNames            = builder.panelNames;
        this.panelWeights          = builder.panelWeights;
        this.panelWeightMap        = builder.panelWeightMap;
        this.numObjectivesPerPanel = builder.numObjectivesPerPanel;
        this.panelDescriptions     = builder.panelDescriptions;
        this.numPanels             = builder.numPanels;
        this.objWeights            = builder.objWeights;
        this.objNames              = builder.objNames;
        this.objectiveDescriptions = builder.objectiveDescriptions;
        this.subobjWeights         = builder.subobjWeights;
        this.subobjectives         = builder.subobjectives;
        this.subobjDescriptions    = builder.subobjDescriptions;
        this.subobjWeightsMap      = builder.subobjWeightsMap;

        this.requestMode                 = builder.requestMode;
        this.parameterList               = builder.parameterList;
        this.subobjectivesToMeasurements = builder.subobjectivesToMeasurements;
        this.measurementsToSubobjectives = builder.measurementsToSubobjectives;
        this.instrumentsToMeasurements   = builder.instrumentsToMeasurements;
        this.measurementsToInstruments   = builder.measurementsToInstruments;

        this.instrumentList  = builder.instrumentList;
        this.orbitList       = builder.orbitList;
        this.numInstr        = builder.instrumentList.length;
        this.numOrbits       = builder.orbitList.length;
        this.MAX_TOTAL_INSTR = this.numOrbits * this.numInstr;

        this.instrumentIndexes = new HashMap<>();
        this.orbitIndexes      = new HashMap<>();

        for (int i = 0; i < numInstr; i++) {
            instrumentIndexes.put(instrumentList[i], i);
        }
        for (int i = 0; i < numOrbits; i++) {
            orbitIndexes.put(orbitList[i], i);
        }

        // orekitResourcesPath = "/Users/gabeapaza/repositories/seakers/design_evaluator/app/src/main/java/vassar/evaluator/coverage/orekit";
        orekitResourcesPath = ResourcePaths.resourcesRootDir + "/orekit"; // DOCKER


        try {
            // FileInputStream fis = new FileInputStream("/Users/gabeapaza/repositories/seakers/design_evaluator/app/problems/smap/dat/revtimes.dat");
            FileInputStream fis = new FileInputStream(ResourcePaths.resourcesRootDir + "/vassar/problems/SMAP/dat/revtimes.dat"); // DOCKER
            ObjectInputStream ois = new ObjectInputStream(fis);
            this.revtimes = (HashMap<String, HashMap<String, Double>>) ois.readObject();
            ois.close();
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }

        this.subobjectiveDetails = new HashMap<>();
        for(ArrayList<ArrayList<String>> panels: this.subobjectives){
            for(ArrayList<String> objectives: panels){
                for(String subobjective: objectives){
                    this.subobjectiveDetails.put(subobjective, client.getSubobjectiveAttributeInformation(subobjective));
                }
            }
        }
    }


    public String[] getOrbitList(){
        return this.orbitList;
    }
    public String[] getInstrumentList(){
        return this.instrumentList;
    }
    public int getNumOrbits(){
        return this.numOrbits;
    }
    public int getNumInstr(){
        return this.numInstr;
    }
    public HashMap<String, Integer> getOrbitIndexes() {
        return orbitIndexes;
    }
    public HashMap<String, Integer> getInstrumentIndexes() {
        return instrumentIndexes;
    }

    public String getRequestMode(){ return requestMode; }


}

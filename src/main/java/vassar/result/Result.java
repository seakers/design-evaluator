package vassar.result;

/**
 *
 * @author Ana-Dani
 */

import com.google.gson.JsonObject;
import org.checkerframework.checker.units.qual.A;
import vassar.architecture.ADDArchitecture;
import vassar.architecture.AbstractArchitecture;

import jess.*;
import vassar.jess.Resource;

import java.io.Serializable;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Vector;

public class Result implements Serializable {
    private static final long serialVersionUID = 1L;

    public double science;
    public double cost;
    public double dataContinuity;
    public double fairnessScore;
    public double programmaticRisk;
    public Integer eval_idx;
    public ArrayList<ArrayList<ArrayList<Double>>> subobjectiveScores;
    public ArrayList<ArrayList<Double>> objectiveScores;
    public ArrayList<Double> panelScores;
    public FuzzyValue fuzzyScience;
    public FuzzyValue fuzzyCost;
    public AbstractArchitecture arch;
    public TreeMap<String,ArrayList<Fact>> explanations;
    public TreeMap<String,ArrayList<Fact>> capabilityList;
    public TreeMap<String,Double> subobjectiveScoresMap;
    public ArrayList<Fact> capabilities;
    public ArrayList<Fact> costFacts;
    public String taskType;

    private Vector<String> performanceCritique;
    private Vector<String> costCritique;

    public String designString;

    public String mission_launch_mass;

    public JsonObject subobjectiveInfo;


    //Constructors
    public Result(){
        this.panelScores = new ArrayList<>();
        this.objectiveScores = new ArrayList<>();
        this.subobjectiveScores = new ArrayList<>();
        this.explanations = new TreeMap<>();
        this.capabilityList = new TreeMap<>();
        this.subobjectiveScoresMap = new TreeMap<>();
        this.capabilities = new ArrayList<>();
        this.costFacts = new ArrayList<>();
        this.performanceCritique = new Vector<>();
        this.costCritique = new Vector<>();
    }

    public Result(AbstractArchitecture arch, double science, double cost) {
        this.science = science;
        this.cost = cost;
        this.subobjectiveScores = null;
        this.subobjectiveScoresMap = null;
        this.objectiveScores = null;
        this.panelScores = null;
        this.arch = arch;
        explanations = null;
        capabilities = null;
        costFacts = null;
        taskType = "Fast";
        this.fuzzyScience = null;
        this.fuzzyCost = null;

        this.dataContinuity = -1;
        this.fairnessScore  = -1;

        this.eval_idx = null;
    }

    public Result(AbstractArchitecture arch,
                  double science,
                  double cost,
                  FuzzyValue fuzzy_science,
                  FuzzyValue fuzzy_cost,
                  ArrayList<ArrayList<ArrayList<Double>>> subobj_scores,
                  ArrayList<ArrayList<Double>> obj_scores,
                  ArrayList<Double> panel_scores,
                  TreeMap<String,Double> subobj_scores_map){

        this.arch = arch;
        this.science = science;
        this.cost = cost;
        this.fuzzyScience = fuzzy_science;
        this.fuzzyCost = fuzzy_cost;
        this.subobjectiveScores = subobj_scores;
        this.objectiveScores = obj_scores;
        this.panelScores = panel_scores;
        this.subobjectiveScoresMap = subobj_scores_map;
        this.designString = "";
        this.subobjectiveInfo = new JsonObject();
        this.mission_launch_mass = "";

        this.eval_idx = null;
    }

    public Result(AbstractArchitecture arch,
                  double science,
                  double cost,
                  ArrayList<ArrayList<ArrayList<Double>>> subobj_scores,
                  ArrayList<ArrayList<Double>> obj_scores,
                  ArrayList<Double> panel_scores,
                  TreeMap<String,Double> subobj_scores_map) {

        this.arch = arch;
        this.science = science;
        this.cost = cost;
        this.fuzzyScience = null;
        this.fuzzyCost = null;
        this.subobjectiveScores = subobj_scores;
        this.objectiveScores = obj_scores;
        this.panelScores = panel_scores;
        this.subobjectiveScoresMap = subobj_scores_map;
        this.designString = "";
        this.subobjectiveInfo = new JsonObject();
        this.mission_launch_mass = "";

        this.eval_idx = null;
    }

    public void setPanelScores(ArrayList<Double> panelScores){
        this.panelScores = panelScores;
    }


    public void setProgrammaticRisk(double programmaticRisk){
        this.programmaticRisk = programmaticRisk;
    }


    public void setSubobjectiveInfo(JsonObject subobjectiveInfo){
        this.subobjectiveInfo = subobjectiveInfo;
    }

    //Getters and Setters
    public void setPerformanceCritique(Vector<String> critique){
        this.performanceCritique = critique;
    }
    public Vector<String> getPerformanceCritique(){
        return this.performanceCritique;
    }

    public void setCostCritique(Vector<String> critique){
        this.costCritique = critique;
    }
    public Vector<String> getCostCritique(){
        return this.costCritique;
    }

    public ArrayList<Fact> getCapabilities() {
        return capabilities;
    }
    public void setCapabilities(ArrayList<Fact> capabilities) {
        this.capabilities = capabilities;
    }

    public TreeMap<String,ArrayList<Fact>> getExplanations() {
        return explanations;
    }
    public void setExplanations(TreeMap<String,ArrayList<Fact>> explanations) {
        this.explanations = explanations;
    }

    public String getTaskType() {
        return taskType;
    }
    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public AbstractArchitecture getArch() {
        return arch;
    }
    public void setArch(AbstractArchitecture arch) {
        this.arch = arch;
    }


    public void setScience(double science) {
        this.science = science;
    }


    public void setCost(double cost) {
        this.cost = cost;
    }

    public void setDesignString(String designString){
        this.designString = designString;
    }
    public String getDesignString(){
        return this.designString;
    }

    public TreeMap<String,ArrayList<Fact>> getCapabilityList() {
        return capabilityList;
    }
    public void setCapabilityList(TreeMap<String,ArrayList<Fact>> capabilityList) {
        this.capabilityList = capabilityList;
    }

    public ArrayList<ArrayList<ArrayList<Double>>> getSubobjectiveScores() {
        return subobjectiveScores;
    }
    public ArrayList<ArrayList<Double>> getObjectiveScores() {
        return objectiveScores;
    }
    public TreeMap<String, Double> getSubobjectiveScoresMap() {
        return subobjectiveScoresMap;
    }
    public ArrayList<Double> getPanelScores() {
        return panelScores;
    }

    public ArrayList<Fact> getCostFacts() {
        return costFacts;
    }
    public void setCostFacts(ArrayList<Fact> cost_facts) {
        this.costFacts = cost_facts;
    }

    public FuzzyValue getFuzzyScience() {
        return fuzzyScience;
    }
    public FuzzyValue getFuzzyCost() {
        return fuzzyCost;
    }

    public void setFuzzyScience(FuzzyValue fuzzyScience) {
        this.fuzzyScience = fuzzyScience;
    }
    public void setFuzzyCost(FuzzyValue fuzzyCost) {
        this.fuzzyCost = fuzzyCost;
    }


    public void setDataContinuityScore(double dataContinuity){
        this.dataContinuity = dataContinuity;
    }


    public void setFairnessScore(double fairnessScore){
        this.fairnessScore = fairnessScore;
    }





    // a: array of subobjective weights
    // b: array of subobjective scores
    // ||b|| == ||a||
    public static double sumProduct(ArrayList<Double> a, ArrayList<Double> b) throws Exception {
        return SumDollar(dotMult(a, b));
    }

    // a: array of subobjective weights
    // b: array of subobjective scores
    // returns c: each element of a and b multiplied together
    // ||b|| == ||a|| == ||c||
    public static ArrayList<Double> dotMult(ArrayList<Double> a, ArrayList<Double> b) throws Exception {
        int n1 = a.size();
        int n2 = b.size();
        if (n1 != n2) {
            throw new Exception("dotSum: Arrays of different sizes");
        }
        ArrayList<Double> c = new ArrayList<>(n1);
        for (int i = 0; i < n1; i++) {
            Double t = a.get(i) * b.get(i);
            c.add(t);
        }
        return c;
    }

    public static double SumDollar(ArrayList<Double> a) {
        double res = 0.0;
        for (Double num: a) {
            res += num;
        }
        return res;
    }


    public HashMap<String, Double> get_panel_satisfaction(Resource res){
        HashMap<String, Double> satisfaction = new HashMap<>();
        for (int panel_idx = 0; panel_idx < res.problem.panelNames.size(); ++panel_idx){
            satisfaction.put(
                    res.problem.panelNames.get(panel_idx),
                    this.getPanelScores().get(panel_idx)
            );
        }
        return satisfaction;
    }




    public Double getPanelScore(int panel_idx){
        if(panel_idx < this.panelScores.size()){
            return this.panelScores.get(panel_idx);
        }
        return 0.0;
    }

    public Double getObjectiveScore(int panel_idx, int objective_idx){
        if(panel_idx < this.objectiveScores.size()){
            if(objective_idx < this.objectiveScores.get(panel_idx).size()){
                return this.objectiveScores.get(panel_idx).get(objective_idx);
            }
        }
        return 0.0;
    }

    public Double getSubobjectiveScore(int panel_idx, int objective_idx, int subobjective_idx){
        if(panel_idx < this.subobjectiveScores.size()){
            if(objective_idx < this.subobjectiveScores.get(panel_idx).size()){
                if(subobjective_idx < this.subobjectiveScores.get(panel_idx).get(objective_idx).size()){
                    return this.subobjectiveScores.get(panel_idx).get(objective_idx).get(subobjective_idx);
                }
            }
        }
        return 0.0;
    }

    @Override
    public String toString() {
        String fs;
        if (fuzzyScience == null)
            fs = "null";
        else
            fs = fuzzyScience.toString();
        String fc;
        if (fuzzyCost == null)
            fc = "null";
        else
            fc = fuzzyCost.toString();
        return "Result{" + "science=" + science + ", cost=" + cost + " fuz_sc=" + fs + " fuz_co=" + fc + ", arch=" + arch.toString() + '}';
    }


//      _____      _   _
//     / ____|    | | | |
//    | |  __  ___| |_| |_ ___ _ __ ___
//    | | |_ |/ _ \ __| __/ _ \ '__/ __|
//    | |__| |  __/ |_| ||  __/ |  \__ \
//     \_____|\___|\__|\__\___|_|  |___/


    public String getCritique(){
        Vector<String> performanceCritique = this.getPerformanceCritique();
        Vector<String> costCritique = this.getCostCritique();
        String critique = " ";
        if(performanceCritique != null){
            for(String crit: performanceCritique){
                critique = critique + crit + " | ";
            }
        }
        if(costCritique != null){
            for(String crit: costCritique){
                critique = critique + crit + " | ";
            }
        }
        return critique;
    }

    public int getEvalIdx(){
        if(this.eval_idx != null){
            return this.eval_idx;
        }
        return 0;
    }

    public double getScience() {
        System.out.println("--> SCIENCE: " + this.science);
        return this.science;
    }

    public double getCost() {
        System.out.println("--> COST: " + this.cost);
        return this.cost;
    }

    public double getDataContinuityScore(){
        System.out.println("--> DATA CONTINUITY: " + this.dataContinuity);
        return this.dataContinuity;
    }

    public double getFairnessScore(){
        System.out.println("--> FAIRNESS: " + this.fairnessScore);
        return this.fairnessScore;
    }

    public double getProgrammaticRisk(){
        System.out.println("--> PROGRAMMATIC RISK: " + this.programmaticRisk);
        return this.programmaticRisk;
    }


}

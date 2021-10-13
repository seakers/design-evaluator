package vassar.result;

/**
 *
 * @author Ana-Dani
 */

import com.google.gson.JsonObject;
import vassar.architecture.ADDArchitecture;
import vassar.architecture.AbstractArchitecture;

import jess.*;

import java.io.Serializable;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Vector;

public class Result implements Serializable {
    private static final long serialVersionUID = 1L;

    private double science;
    private double cost;
    private double dataContinuity;
    private double fairnessScore;
    private double programmaticRisk;
    private ArrayList<ArrayList<ArrayList<Double>>> subobjectiveScores;
    private ArrayList<ArrayList<Double>> objectiveScores;
    private ArrayList<Double> panelScores;
    private FuzzyValue fuzzyScience;
    private FuzzyValue fuzzyCost;
    private AbstractArchitecture arch;
    public TreeMap<String,ArrayList<Fact>> explanations;
    private TreeMap<String,ArrayList<Fact>> capabilityList;
    private TreeMap<String,Double> subobjectiveScoresMap;
    public ArrayList<Fact> capabilities;
    private ArrayList<Fact> costFacts;
    private String taskType;

    private Vector<String> performanceCritique;
    private Vector<String> costCritique;

    public String designString;

    public String mission_launch_mass;

    public JsonObject subobjectiveInfo;


    //Constructors
    public Result(){}

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
    }


    public void setProgrammaticRisk(double programmaticRisk){
        this.programmaticRisk = programmaticRisk;
    }
    public double getProgrammaticRisk(){
        return this.programmaticRisk;
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

    public double getScience() {
        return science;
    }
    public void setScience(double science) {
        this.science = science;
    }

    public double getCost() {
        return cost;
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

    public double getDataContinuityScore(){
        return this.dataContinuity;
    }
    public void setDataContinuityScore(double dataContinuity){
        this.dataContinuity = dataContinuity;
    }

    public double getFairnessScore(){
        return this.fairnessScore;
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

}

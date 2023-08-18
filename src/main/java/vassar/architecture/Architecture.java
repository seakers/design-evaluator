package vassar.architecture;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import vassar.problem.Problem;

import java.io.FileWriter;
import java.util.*;

public class Architecture extends AbstractArchitecture{

    private Problem problem;
    private boolean[][] bitMatrix;
    private int numSatellites;

    public Architecture(String bitString, int numSatellites, Problem problem) {
        super();
        this.problem = problem;
        this.bitMatrix = booleanString2Matrix(bitString);
        this.numSatellites = numSatellites;

        JsonObject arch_inputs = new JsonObject();
        arch_inputs.addProperty("bitString", bitString);
        String orbs = "";
        for(String orb: problem.orbitList){
            orbs += " " + orb;
        }
        String insts = "";
        for(String inst: problem.instrumentList){
            insts += " " + inst;
        }
        arch_inputs.addProperty("orbits", orbs);
        arch_inputs.addProperty("instruments", insts);

        try{
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            FileWriter jsonWriter = new FileWriter("/app/debug/datasets/arch.json");
            String jsonString = gson.toJson(arch_inputs);
            jsonWriter.write(jsonString);
            jsonWriter.flush();
        }
        catch (Exception ex){
            ex.printStackTrace();
        }



    }

    public Architecture(boolean[][] bitMatrix, int numSatellites, Problem problem) {
        super();
        this.problem = problem;
        this.bitMatrix = bitMatrix;
        this.numSatellites = numSatellites;
    }

    public Architecture(HashMap<String, String[]> mapping, int numSatellites, Problem problem) {
        super();
        this.problem = problem;
        bitMatrix = new boolean[problem.getNumOrbits()][problem.getNumInstr()];
        for (int o = 0; o < problem.getNumOrbits(); o++) {
            for(int i = 0; i < problem.getNumInstr(); i++) {
                bitMatrix[o][i] = false;
            }
        }

        for (int o = 0; o < problem.getNumOrbits(); o++) {
            String orb = problem.getOrbitList()[o];
            String[] payl = mapping.get(orb);
            if (payl == null)
                continue;
            ArrayList<String> thepayl = new ArrayList<>(Arrays.asList(payl));
            for(int i = 0; i < problem.getNumInstr(); i++) {
                String instr = problem.getInstrumentList()[i];
                if(thepayl.contains(instr))
                    bitMatrix[o][i] = true;
            }
        }
        this.numSatellites = numSatellites;
    }

    @Override
    public boolean isFeasibleAssignment() {
        return (sumAllInstruments(bitMatrix) <= problem.MAX_TOTAL_INSTR);
    }

    public int getNumSatellites() {
        return numSatellites;
    }

    public boolean[][] getBitMatrix(){
        return this.bitMatrix;
    }

    // Utils
    private boolean[][] booleanString2Matrix(String bitString) {
        boolean[][] mat = new boolean[problem.getNumOrbits()][problem.getNumInstr()];
        System.out.println("ORBITS: " + problem.getNumOrbits());
        System.out.println("INSTRUMENTS: " + problem.getNumInstr());
        for (int i = 0; i < problem.getNumOrbits(); i++) {
            for (int j = 0; j < problem.getNumInstr(); j++) {
                String b = bitString.substring(problem.getNumInstr() *i + j,problem.getNumInstr()*i + j + 1);
                if (b.equalsIgnoreCase("1")) {
                    mat[i][j] = true;
                }
                else if (b.equalsIgnoreCase("0")) {
                    mat[i][j] = false;
                }
                else {
                    System.out.println("Architecture: booleanString2Matrix string b is nor equal to 1 or 0!");
                }
            }
        }
        return mat;
    }

    private int sumAllInstruments(boolean[][] mat) {
        int x = 0;
        for (boolean[] row: mat) {
            for (boolean val: row) {
                if (val) {
                    x += 1;
                }
            }
        }
        return x;
    }

    @Override
    public String toString(String delimiter){
        StringJoiner sj = new StringJoiner(delimiter);

        for(int i = 0; i < problem.getNumOrbits(); i++){
            for(int j = 0; j < problem.getNumInstr(); j++){
                String k;
                if(bitMatrix[i][j]){
                    k = "1";
                }else{
                    k = "0";
                }
                sj.add(k);
            }
        }
        return sj.toString();
    }
}


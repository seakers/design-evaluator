package vassar.combinatorics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class NDSM2 implements Serializable {


    public String[] elements;
    public String description;

    public HashMap<String[], Double> map;

    public NDSM2(String[] elements, String description){
        this.elements = elements;
        this.description = description;
        this.map = new HashMap<>();
    }

    public void setInteraction(String[] instruments, double value){
        this.map.put(instruments, value);
    }

}

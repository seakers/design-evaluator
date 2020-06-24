package vassar;

import vassar.jess.attribute.EOAttribute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GlobalScope {

    // MEASUREMENT PARAMETERS
    public static HashMap<String, Integer>     measurementAttributeList;
    public static HashMap<Integer, String>     measurementAttributeKeys;
    public static HashMap<String, String>      measurementAttributeTypes;
    public static HashMap<String, EOAttribute> measurementAttributeSet;

    // INSTRUMENT PARAMETERS
    public static HashMap<String, Integer>     instrumentAttributeList;
    public static HashMap<Integer, String>     instrumentAttributeKeys;
    public static HashMap<String, String>      instrumentAttributeTypes;
    public static HashMap<String, EOAttribute> instrumentAttributeSet;

    public static void defineMeasurement(HashMap<String, Integer> attribs, HashMap<Integer, String> attribKeys,
                                         HashMap<String, String> attribTypes, HashMap<String, EOAttribute> attribSet) {
        measurementAttributeList = attribs;
        measurementAttributeKeys = attribKeys;
        measurementAttributeTypes = attribTypes;
        measurementAttributeSet = attribSet;
    }
    public static void defineInstrument(HashMap<String, Integer> attribs, HashMap<Integer, String> attribKeys,
                                        HashMap<String, String> attribTypes, HashMap<String, EOAttribute> attribSet){
        instrumentAttributeList = attribs;
        instrumentAttributeKeys = attribKeys;
        instrumentAttributeTypes = attribTypes;
        instrumentAttributeSet = attribSet;
    }

    // SUBOBJECTIVES
    public static HashMap<String, String>            subobjectivesToMeasurements;
    public static HashMap<String, ArrayList<String>> measurementsToSubobjectives;


    public static void buildMeasurementsToSubobjectives(){
        HashMap<String, ArrayList<String>> inverse = new HashMap<>();
        for (Map.Entry<String, String> entr: subobjectivesToMeasurements.entrySet()) {
            String key = entr.getKey();
            String val = entr.getValue();
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
        measurementsToSubobjectives = inverse;
    }

    public static void init(){
        measurementsToSubobjectives = new HashMap<>();
        subobjectivesToMeasurements = new HashMap<>();
    }


}

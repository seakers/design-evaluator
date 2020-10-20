package evaluator;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.File;

public class Files {

    public static String output_dir = "/app/output/";

    public static String ndsm2 = Files.output_dir + "DSM-2-2020-10-10-19-06-58" + ".dat";
    public static String ndsm3 = Files.output_dir + "DSM-3-2020-10-10-19-11-12" + ".dat";
    public static String ndsmN = Files.output_dir + "DSM-5-2020-10-10-19-12-04" + ".dat";


    public static String ndsm2_Dacadal = Files.output_dir + "DSM-Decadal2-2020-10-13-22-54-47" + ".dat";




//
//    public static void writeJsonMeasurementFile(int start_year, int end_year, int measurement_start_yr, int measurement_end_yr, String vassar_meas_name, String historical_meas_name){
//        Gson gson = new GsonBuilder().setPrettyPrinting().create();
//        String directory = "/app/output_data_continuity";
//
//        int file_count = (new File(directory)).list().length;
//        String file_name = "pop_" + file_count + "_designs.json";
//        String file_path = directory + "/" + file_name;
//
//
//        JsonObject file_obj = gson.fromJson(file_path);
//
//
//
//
//    }







}

package vassar.database.service;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import org.json.simple.JSONObject;

import evaluator.ResourcePaths;

// I/O
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.BufferedWriter;


public class DebugAPI {

    private File           logFile;
    private BufferedWriter writer;
    private JSONObject     json;
    private FileWriter     jsonWriter;
    private String         outputFilePath; // = "/app/logs/jessInitDB.json"; ???
    private String         outputPath;


    public static class Builder {

        private File           logFile;
        private BufferedWriter writer;
        private JSONObject     json;
        private FileWriter     jsonWriter;
        private String         outputFilePath; // = "/app/logs/jessInitDB.json"; ???
        private String         outputPath;

        public Builder(String outputFilePath){
            try {
                this.outputFilePath = outputFilePath;
                this.logFile        = new File(this.outputFilePath);
                this.json           = new JSONObject();
                if (this.logFile.exists()) {
                    this.writer         = new BufferedWriter(new FileWriter(this.outputFilePath, true));
                    this.jsonWriter     = new FileWriter(this.outputFilePath);
                }
            }
            catch (Exception e) {
                System.out.println("EXC in DebugAPI constructor " +e.getClass() + " : " + e.getMessage());
                e.printStackTrace();
            }
        }

        public Builder newFile(){
            try {
                if(!this.logFile.createNewFile()){
                    this.logFile.delete();
                    this.logFile.createNewFile();
                    this.logFile.setWritable(true, false);
                    this.logFile.setExecutable(true, false);
                    this.writer     = new BufferedWriter(new FileWriter(this.outputFilePath, true));
                    this.jsonWriter = new FileWriter(this.outputFilePath);
                }
            }
            catch (Exception e) {
                System.out.println("EXC in DebugAPI constructor " + e.getClass() + " : " + e.getMessage());
                e.printStackTrace();
            }
            return this;
        }

        public Builder setOutputPath(String outputPath) {
            this.outputPath = outputPath;
            // Ensure outputPath exists
            try {
                Path outputPathPath = Path.of(outputPath);
                Files.createDirectories(outputPathPath);
            }
            catch (IOException e) {
                System.out.println("EXC in DebugAPI constructor " + e.getClass() + " : " + e.getMessage());
                e.printStackTrace();
            }
            
            return this;
        }

        public DebugAPI build() {
            DebugAPI client    = new DebugAPI();

            client.logFile        = this.logFile;
            client.writer         = this.writer;
            client.json           = this.json;
            client.jsonWriter     = this.jsonWriter;
            client.outputFilePath = this.outputFilePath; // = "/app/logs/jessInitDB.json"; ???
            client.outputPath     = this.outputPath;

            return client;
        }
    }

    public void putJson(String key, Object value) {
        JsonArray json_value  = new Gson().toJsonTree(value).getAsJsonArray();
        this.json.put(key, json_value);
    }

    public void writeJson() {
        try {
            System.out.println("Writing JSON to " + this.outputFilePath);
            String jsonString = this.json.toJSONString();
            this.jsonWriter.write(jsonString);
            this.jsonWriter.flush();
        }
        catch (Exception e) {
            System.out.println("EXC in DatabaseClient writeJson " +e.getClass() + " : " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void writeTemplateOutputFile(String path, String content) {
        String[] dirs = path.split("/");
        String fileName = "/" + dirs[dirs.length-1] + ".txt";

        File outputFile = new File(this.outputPath + fileName);
        try{
            if(!outputFile.createNewFile()) {
                outputFile.delete();
                outputFile.createNewFile();
                outputFile.setWritable(true);
                outputFile.setReadable(true);
                FileWriter outputWriter = new FileWriter(this.outputPath + fileName);
                outputWriter.write(content);
                outputWriter.close();
            }
            else{
                outputFile.setWritable(true);
                outputFile.setReadable(true);
                FileWriter outputWriter = new FileWriter(this.outputPath + fileName);
                outputWriter.write(content);
                outputWriter.close();
            }
        }
        catch (Exception e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public void writeTemplateOutputFileName(String fileName, String content) {

        if(ResourcePaths.writeFiles){
            fileName = "/" + fileName + ".txt";
            File outputFile = new File(this.outputPath + fileName);
            try{
                System.out.println("--> FULL FILE PATH: " + this.outputPath + fileName);
                if(!outputFile.createNewFile()) {
                    outputFile.delete();
                    outputFile.createNewFile();
                    outputFile.setWritable(true);
                    outputFile.setReadable(true);
                    FileWriter outputWriter = new FileWriter(this.outputPath + fileName);
                    outputWriter.write(content);
                    outputWriter.close();
                }
                else{
                    outputFile.setWritable(true);
                    outputFile.setReadable(true);
                    FileWriter outputWriter = new FileWriter(this.outputPath + fileName);
                    outputWriter.write(content);
                    outputWriter.close();
                }
            }
            catch (Exception e) {
                System.out.println("An error occurred writing: " + fileName);
                e.printStackTrace();
            }
        }
    }


    // file_name: contains the full path to the file
    public void writeTemplateOutputFileDir(String file_name, String content) {

        File outputFile = new File(file_name);
        try{
            if(!outputFile.createNewFile()) {
                outputFile.delete();
                outputFile.createNewFile();
                outputFile.setWritable(true);
                outputFile.setReadable(true);
                FileWriter outputWriter = new FileWriter(file_name);
                outputWriter.write(content);
                outputWriter.close();
            }
            else{
                outputFile.setWritable(true);
                outputFile.setReadable(true);
                FileWriter outputWriter = new FileWriter(file_name);
                outputWriter.write(content);
                outputWriter.close();
            }
        }
        catch (Exception e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }




}

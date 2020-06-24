package vassar.database;

// I/O

// JSON
import vassar.database.service.DebugAPI;
import vassar.database.service.QueryAPI;
import vassar.database.template.TemplateRequest;
import vassar.database.template.TemplateResponse;
import vassar.problem.Problem;


public class DatabaseClient {

    private boolean  debug;
    private QueryAPI queryAPI;
    private DebugAPI debugAPI;

    public static class Builder {

        private boolean  debug;
        private QueryAPI queryAPI;
        private DebugAPI debugAPI;

        public Builder(){
            this.debug = false;
            this.queryAPI = null;
        }
        public Builder debug(boolean debug){
            this.debug = debug;
            return this;
        }

        public Builder queryClient(QueryAPI qClient){
            this.queryAPI = qClient;
            return this;
        }

        public Builder debugClient(DebugAPI dClient){
            this.debugAPI = dClient;
            return this;
        }

        public DatabaseClient build() {
            DatabaseClient api    = new DatabaseClient();

            api.debug    = this.debug;
            api.queryAPI = this.queryAPI;
            api.debugAPI = this.debugAPI;

            return api;
        }
    }

    public TemplateResponse processTemplateRequest(TemplateRequest request, Problem.Builder problemBuilder) {

        // PASS PROBLEM
        request.setProblemBuilder(problemBuilder);
        String key = request.getTemplateFilePath();

        // PROCESS REQUEST
        TemplateResponse response = request.processRequest(this.queryAPI);
        System.out.println(key);
        this.debugAPI.writeTemplateOutputFile(key, response.getTemplateString());

        return response;
    }




    public int indexArchitecture(String input, double science, double cost){
        return this.queryAPI.insertArchitecture(input, science, cost);
    }


    public void indexArchitectureScoreExplanation(String panelName, double satisfaction, double weight, int archID){
        System.out.println("---> Getting panel id: " + panelName);
        int panelId = this.queryAPI.getPanelID(panelName);
        this.queryAPI.insertArchitectureScoreExplanation(archID, panelId, satisfaction);
    }

    public void indexPanelScoreExplanation(String objName, double satisfaction, double weight, int archID){
        System.out.println("---> Getting objective id: " + objName);
        int objectiveId = this.queryAPI.getObjectiveID(objName);
        this.queryAPI.insertPanelScoreExplanation(archID, objectiveId, satisfaction);
    }

    public void indexObjectiveScoreExplanation(String subobjName, double satisfaction, double weight, int archID){
        System.out.println("---> Getting subobjective id: " + subobjName);
        int subobjectiveID = this.queryAPI.getSubobjectiveID(subobjName);
        this.queryAPI.insertObjectiveScoreExplanation(archID, subobjectiveID, satisfaction);
    }






    public void setProblemID(int id){
        this.queryAPI.problem_id = id;
    }

    public void setGroupID(int id){
        this.queryAPI.group_id = id;
    }


    public void writeDebugInfo() {
        this.debugAPI.writeJson();
    }

}

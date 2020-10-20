package vassar.jess.modules;

import jess.Rete;
import vassar.architecture.ADDArchitecture;
import vassar.database.DatabaseClient;
import vassar.jess.QueryBuilder;

public class Performance {


    public static class Builder{

        private Rete engine;
        private DatabaseClient dbClient;
        private QueryBuilder q_builder;
        private ADDArchitecture arch;

        public Builder(Rete engine, DatabaseClient dbClient, boolean output){
            this.engine = engine;
            this.dbClient = dbClient;
            if(output){
                try{
                    this.engine.eval("(watch rules)");
                    this.engine.eval("(facts)");
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

        public Builder setQueryBuilder(QueryBuilder q_builder){
            this.q_builder = q_builder;
            return this;
        }

        public Builder setArchitecture(ADDArchitecture arch){
            this.arch = arch;
            return this;
        }

        public Builder orbitSelection(){
            return this;
        }

        public Builder synergies(){
            return this;
        }

        public Performance build(boolean select_orbits, boolean declare_synergies){
            Performance build = new Performance();

            try{
                this.engine.eval("(bind ?*science-multiplier* 1.0)");
            }
            catch (Exception e){
                e.printStackTrace();
            }



            return build;
        }



    }





}

package vassar.architecture;

import evaluator.EvaluatorApp;
import jess.Fact;
import jess.JessException;
import jess.Rete;
import org.checkerframework.checker.units.qual.A;
import vassar.evaluator.spacecraft.Orbit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SingleSat extends ADDArchitecture{

    public ArrayList<String> instruments;
    public String orbit;

    public SingleSat(ArrayList<String> instruments){
        super();
        this.instruments = instruments;
        this.orbit = null;
    }

    public SingleSat(ArrayList<String> instruments, String orbit){
        super();
        this.instruments = instruments;
        this.orbit = orbit;
    }

    @Override
    public Set<Orbit> assignArchitecture(Rete engine){
        String mission_name = "ndsm_mission";
        String payload = "";
        HashSet<Orbit> orbitsUsed = null;
        for(String instrument: this.instruments){
            payload += (" " + instrument);
        }

        String mission_fact_str = "";
        if(this.orbit != null){
            Orbit orb = new Orbit(this.orbit, 1, 1);
            orbitsUsed = new HashSet<>();
            orbitsUsed.add(orb);
            mission_fact_str = "(MANIFEST::Mission (Name "+mission_name+") (instruments " + payload + ") (lifetime 5) (launch-date 2015) " + orb.toJessSlots() + " (select-orbit no) (order-index 0) (factHistory F0))";
        }
        else{
            mission_fact_str = "(MANIFEST::Mission (Name "+mission_name+") (instruments " + payload + ") (lifetime 5) (launch-date 2015) (select-orbit yes) (order-index 0) (factHistory F0))";
        }


        try {
            Fact mission_fact = engine.assertString(mission_fact_str);
            // Fact synergy_fact = engine.assertString(synergy_fact_str);
            // this.mission_fact_ordering.add(mission_fact);
            // this.synergy_fact_ordering.add(synergy_fact);

        } catch (JessException e) {
            e.printStackTrace();
        }

        return orbitsUsed;
    }

    @Override
    public void setMissionOrbit(String orbit, ArrayList<String> mission_instruments){
        // this.orbit = new Orbit(orbit, 1, 1);
    }


    @Override
    public boolean isFeasibleAssignment(){
        if(this.instruments.isEmpty()){
            return false;
        }
        return true;
    }

    @Override
    public ArrayList<String> getOrbitsUsed(){
        ArrayList<String> orbits = new ArrayList<>();
        orbits.add(this.orbit);
        return orbits;
    }

    @Override
    public String toString(String delimiter){
        return this.instruments.toString();
    }






}

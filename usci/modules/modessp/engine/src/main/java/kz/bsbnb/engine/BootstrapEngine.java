package kz.bsbnb.engine;

import kz.bsbnb.DataEntity;
import kz.bsbnb.annotations.InfoBootstrap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BootstrapEngine {

    @Autowired
    private DatabaseActivity databaseActivity;

    @Autowired
    PrepareEngine prepareEngine;

    @Autowired
    LoadHistoryDecision loadHistoryDecision;

    @Autowired
    NewEntityProcessDecision newEntityProcessDecision;

    @Autowired
    RefEngine refEngine;

    @InfoBootstrap
    public DataEntity process(DataEntity entity){
        DataEntity prepared = prepareEngine.process(entity);
        refEngine.process(prepared);

        if(prepared.getId() > 0) {
            return loadHistoryDecision
                    .withSaving(prepared)
                    .make();

        } else {
            return newEntityProcessDecision
                    .withSaving(prepared)
                    .make();
        }



        /*List<Decision> decisionList = new ArrayList<>();
        decisionList.add(new LoadHistoryDecision()
                        .with(entity));

        while(decisionList.size() > 0) {
            Decision decision = decisionList.get(0);
            decision.make(decisionList);
        }*/

    }

    public DatabaseActivity getDatabaseActivity() {
        return databaseActivity;
    }
}

package kz.bsbnb.engine;

import kz.bsbnb.DataEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PrepareEngine {

    @Autowired
    DatabaseActivity activity;

    @Autowired
    SavingInfo savingInfo;

    @Autowired
    NewEntityProcessDecision newEntityProcessDecision;

    public DataEntity process(DataEntity entity){
        activity.select();
        System.out.println("prepare for creditorId: "+ savingInfo.getCreditorId());
        newEntityProcessDecision.process();
        return entity;
    }

    public PrepareEngine() {
        //System.out.println("const");
    }
}

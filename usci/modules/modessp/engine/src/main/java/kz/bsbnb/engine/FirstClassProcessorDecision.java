package kz.bsbnb.engine;

import kz.bsbnb.DataEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FirstClassProcessorDecision extends Decision {

    @Autowired
    DatabaseActivity databaseActivity;

    @Override
    public DataEntity make() {
        if(loadEntity.isOneRow(savingEntity))
            return loadEntity;
        else {
            dataEntityDao.insert(savingEntity);
            return savingEntity;
        }

    }
}

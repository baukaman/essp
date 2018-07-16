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
        if(loadedEntity.isOneRow(savingEntity))
            return loadedEntity;
        else {
            dataEntityDao.insert(savingEntity);
            return savingEntity;
        }

    }
}

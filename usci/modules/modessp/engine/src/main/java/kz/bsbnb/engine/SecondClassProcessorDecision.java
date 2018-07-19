package kz.bsbnb.engine;

import kz.bsbnb.DataEntity;
import org.springframework.stereotype.Component;

@Component
public class SecondClassProcessorDecision extends Decision {

    @Override
    public DataEntity make() {
        if(!savingEntity.subsetOf(loadedEntity)) {
            dataEntityDao.update(savingEntity);
            return savingEntity;
        } else {
            return loadedEntity;
        }
    }
}

package kz.bsbnb.engine;

import kz.bsbnb.DataEntity;
import org.springframework.stereotype.Component;

@Component
public class ThirdClassProcessorDecision extends Decision {
    @Override
    public DataEntity make() {
        if(!savingEntity.subsetOf(loadedEntity)) {
            dataEntityDao.insert(savingEntity);
            return savingEntity;
        } else {
            dataEntityDao.updateReportDate(loadedEntity, savingEntity.getReportDate());
            loadedEntity.setReportDate(savingEntity.getReportDate());
            return loadedEntity;
        }
    }
}

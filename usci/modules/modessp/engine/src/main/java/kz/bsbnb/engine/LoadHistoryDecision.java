package kz.bsbnb.engine;

import com.google.common.base.Optional;
import kz.bsbnb.DataEntity;
import kz.bsbnb.dao.DataEntityDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LoadHistoryDecision {
    DataEntity entity = new DataEntity(null);

    @Autowired
    FirstClassProcessorDecision firstClassProcessorDecision;

    @Autowired
    DataEntityDao dao;

    public DataEntity make(DataEntity entity) {
        Optional<DataEntity> loadedEntityOptional = dao.loadByMaxReportDate(entity);
        DataEntity applied;

        if(loadedEntityOptional.isPresent()) {
            DataEntity loadedEntity = loadedEntityOptional.get();

            if (loadedEntity.getReportDate().compareTo(entity.getReportDate()) < 0) {
                applied = firstClassProcessorDecision
                        .withLoaded(loadedEntity)
                        .withSaving(entity)
                        .make();
            } else {
                throw new UnsupportedOperationException();
            }
        } else {
            throw new UnsupportedOperationException();
        }

        return applied;
    }

    /*public void make(List<Decision> decisionList){
        if(entity.getId() > 0) {
            DataEntity loadedEntity = dao.loadByMaxReportDate(entity);

            if(loadedEntity.getReportDate().compareTo(entity) < 0) {
                decisionList.add(new FirstClassProcessorDecision()
                        .withDbAcitivity(databaseActivity)
                        .withLoadedEntity(loadedEntity)
                        .withSavingEntity(entity));
            } else if(loadedEntity.getReportDate().compareTo(entity) == 0) {
                decisionList.add(new SecondClassProcessorDecision()
                        .withLoadedEntity(loadedEntity)
                        .withSavingEntity(entity));
            } else {
                loadedEntity = dao.loadByMinReporDate(entity);
                decisionList.add(new ThirdClassProcessDecision()
                            .withLoadedEntity(loadedEntity)
                            .withSavingEntity(entity));
            }
        } else {
            decisionList.add(new NewEntityProcessDecision()
                            .withSavingEntity(entity)
                            .withDbActivity(databaseActivity));
        }

    }*/
}

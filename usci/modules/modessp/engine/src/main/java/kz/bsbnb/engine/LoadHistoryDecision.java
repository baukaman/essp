package kz.bsbnb.engine;

import com.google.common.base.Optional;
import kz.bsbnb.DataEntity;
import kz.bsbnb.dao.DataEntityDao;
import kz.bsbnb.exception.RefLoadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class LoadHistoryDecision extends Decision {
    //DataEntity entity = new DataEntity(null);

    @Autowired
    FirstClassProcessorDecision firstClassProcessorDecision;

    @Autowired
    SecondClassProcessorDecision secondClassProcessorDecision;

    @Autowired
    ThirdClassProcessorDecision thirdClassProcessorDecision;

    @Autowired
    DataEntityDao dao;

    @Override
    public DataEntity make() throws RefLoadException {
        Optional<DataEntity> loadedEntityOptional = dao.loadByMaxReportDate(savingEntity);
        DataEntity applied;

        if(loadedEntityOptional.isPresent()) {
            DataEntity loadedEntity = loadedEntityOptional.get();

            if (loadedEntity.getReportDate().compareTo(savingEntity.getReportDate()) < 0) {
                applied = firstClassProcessorDecision
                        .withLoaded(loadedEntity)
                        .withSaving(savingEntity)
                        .make();
            } else if(loadedEntity.getReportDate().compareTo(savingEntity.getReportDate()) == 0){
                applied = secondClassProcessorDecision
                        .withLoaded(loadedEntity)
                        .withSaving(savingEntity)
                        .make();
            } else
                throw new UnsupportedOperationException();
        } else {
            loadedEntityOptional = dao.loadByMinReportDate(savingEntity);
            if(loadedEntityOptional.isPresent()) {
                applied = thirdClassProcessorDecision
                        .withLoaded(loadedEntityOptional.get())
                        .withSaving(savingEntity)
                        .make();
            } else {
                throw new IllegalStateException();
            }
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

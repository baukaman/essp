package kz.bsbnb.engine;

import kz.bsbnb.DataEntity;
import kz.bsbnb.dao.DataEntityDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
public class NewEntityProcessDecision {
    DataEntity savingEntity;

    @Autowired
    SavingInfo savingInfo;

    @Autowired
    DatabaseActivity activity;

    public void process() {
        activity.select();
        System.out.println("NewEntityProcessorDecision: creditorId = " + savingInfo.getCreditorId() + " " + savingInfo.getId());
        //System.out.println(Thread.currentThread().getName());
        //myDao.save(savingEntity);

        //then cascade to children
        /*
              for(String attribute: savingEntity.attributes()) {
                  IMetaAttribute attribute = savingEntity.get(attribute);
                  MetaType metaType = attribute.getType();
                  if(metaType.isComplex()) {
                    new LoadHistoryDecision()
                            .withSavingEntity(entity.get(attribute));
                  }

              }
         */



    }
}

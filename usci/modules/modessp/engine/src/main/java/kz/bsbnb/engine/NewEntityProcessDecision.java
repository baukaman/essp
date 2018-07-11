package kz.bsbnb.engine;

import kz.bsbnb.DataEntity;
import kz.bsbnb.dao.DataEntityDao;
import kz.bsbnb.usci.eav.model.meta.IMetaAttribute;
import kz.bsbnb.usci.eav.model.meta.IMetaType;
import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class NewEntityProcessDecision extends Decision {

    @Autowired
    SavingInfo savingInfo;

    @Autowired
    DatabaseActivity activity;

    @Autowired
    DataEntityDao dataEntityDao;

    @Autowired
    ApplicationContext context;

    @Override
    public DataEntity make() {
        MetaClass metaClass = savingEntity.getMeta();

        for (String attribute : savingEntity.getAttributes()) {
            IMetaAttribute metaAttribute = metaClass.getMetaAttribute(attribute);
            IMetaType metaType = metaAttribute.getMetaType();
            Object value = savingEntity.getBaseValue(attribute).getValue();

            if(metaType.isComplex()) {
                DataEntity childEntity = (DataEntity) value;
                if(childEntity.getId() < 1) {
                    NewEntityProcessDecision newEntityProcessDecision = context.getBean(NewEntityProcessDecision.class);
                    newEntityProcessDecision.withSaving(childEntity).make();
                }

            }
        }

        dataEntityDao.insert(savingEntity);
        return savingEntity;
    }
}

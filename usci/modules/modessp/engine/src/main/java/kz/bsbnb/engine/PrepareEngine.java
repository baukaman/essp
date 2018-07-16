package kz.bsbnb.engine;

import kz.bsbnb.DataComplexValue;
import kz.bsbnb.DataEntity;
import kz.bsbnb.DataValue;
import kz.bsbnb.SavingInfo;
import kz.bsbnb.dao.SearchEntityDao;
import kz.bsbnb.usci.eav.model.meta.IMetaAttribute;
import kz.bsbnb.usci.eav.model.meta.IMetaType;
import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;
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

    @Autowired
    SearchEntityDao searchEntityDao;

    public DataEntity process(DataEntity entity) {
        MetaClass metaClass = entity.getMeta();
        DataEntity ret = entity.clone();

        boolean childKeysFound = true;
        //prepare children first
        for (String attribute : metaClass.getAttributeNames()) {
            IMetaAttribute metaAttribute = metaClass.getMetaAttribute(attribute);
            IMetaType metaType = metaAttribute.getMetaType();
            DataValue baseValue = entity.getBaseValue(attribute);

            if (!metaAttribute.isKey())
                continue;

            if(baseValue == null) {
                childKeysFound = false;
                continue;
            }

            if(metaType.isComplex()) {
                DataEntity childEntity = process((DataEntity) baseValue.getValue());
                ret.setDataValue(attribute, new DataComplexValue(childEntity));
                if(childEntity.getId() < 1)
                    childKeysFound = false;
            }
        }

        if(childKeysFound) {
            long searchId = searchEntityDao.search(ret);
            ret.setId(searchId);
        }
        return ret;
    }

    public PrepareEngine() {
        //System.out.println("const");
    }
}

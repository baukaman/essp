package kz.bsbnb.engine;

import kz.bsbnb.DataComplexValue;
import kz.bsbnb.DataEntity;
import kz.bsbnb.DataValue;
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
        DataEntity ret = new DataEntity(metaClass);

        //prepare children first
        for (String attribute : entity.getAttributes()) {
            IMetaAttribute metaAttribute = metaClass.getMetaAttribute(attribute);
            IMetaType metaType = metaAttribute.getMetaType();
            DataValue baseValue = entity.getBaseValue(attribute);

            if (!metaAttribute.isKey())
                continue;

            if(metaType.isComplex())
                ret.setDataValue(attribute, new DataComplexValue(process(((DataEntity) baseValue.getValue()))));
        }
        long searchId = searchEntityDao.search(ret);
        ret.setId(searchId);
        return ret;
    }

    public PrepareEngine() {
        //System.out.println("const");
    }
}

package kz.bsbnb.engine.impl;

import kz.bsbnb.DataEntity;
import kz.bsbnb.DataSet;
import kz.bsbnb.engine.IRefEngine;
import kz.bsbnb.exception.RefNotFoundException;
import kz.bsbnb.usci.eav.model.meta.IMetaAttribute;
import kz.bsbnb.usci.eav.model.meta.IMetaType;
import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;

public abstract class AbstractRefEngineImpl implements IRefEngine {
    public void process(DataEntity entity) throws RefNotFoundException {
        MetaClass metaClass = entity.getMeta();
        for (String attribute : entity.getAttributes()) {
            IMetaAttribute metaAttribute = metaClass.getMetaAttribute(attribute);
            IMetaType metaType = metaAttribute.getMetaType();
            Object value = entity.getDataValue(attribute).getValue();

            if(metaType.isComplex()) {
                if (!metaType.isSet()) {
                    DataEntity childEntity = (DataEntity) value;
                    process(childEntity);

                    if(metaType.isReference()) {
                        long refId = getId(childEntity);
                        if(refId < 1)
                            throw new RefNotFoundException(childEntity);
                        childEntity.setId(refId);
                    }
                } else {
                    DataSet childSet = (DataSet) value;
                    for (DataEntity childEntity : childSet.getValues()) {
                        process(childEntity);
                    }
                }
            }
        }
    }
}

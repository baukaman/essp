package kz.bsbnb.engine.impl;

import kz.bsbnb.DataEntity;
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

            if(metaType.isReference()) {
                DataEntity childRefEntity = ((DataEntity) entity.getBaseValue(attribute).getValue());
                long refId = getId(childRefEntity);
                if(refId < 1)
                    throw new RefNotFoundException(childRefEntity);
                childRefEntity.setId(refId);
            }
        }
    }
}

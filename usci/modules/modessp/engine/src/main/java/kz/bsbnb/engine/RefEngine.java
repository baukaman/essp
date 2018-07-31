package kz.bsbnb.engine;

import kz.bsbnb.DataEntity;
import kz.bsbnb.DataValue;
import kz.bsbnb.dao.IRefDao;
import kz.bsbnb.dao.ISearchEntityDao;
import kz.bsbnb.exception.RefNotFoundException;
import kz.bsbnb.usci.eav.model.meta.IMetaAttribute;
import kz.bsbnb.usci.eav.model.meta.IMetaType;
import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RefEngine {

    @Autowired
    ISearchEntityDao searchEntityDao;


    public void process(DataEntity entity) {
        MetaClass metaClass = entity.getMeta();
        for (String attribute : entity.getAttributes()) {
            IMetaAttribute metaAttribute = metaClass.getMetaAttribute(attribute);
            IMetaType metaType = metaAttribute.getMetaType();

            if(metaType.isReference()) {
                DataEntity childRefEntity = ((DataEntity) entity.getBaseValue(attribute).getValue());
                long refId = searchEntityDao.search(childRefEntity);
                if(refId < 1)
                    throw new RefNotFoundException(childRefEntity);
                childRefEntity.setId(refId);
            }
        }
    }

    public void setSearchEntityDao(ISearchEntityDao searchEntityDao) {
        this.searchEntityDao = searchEntityDao;
    }
}

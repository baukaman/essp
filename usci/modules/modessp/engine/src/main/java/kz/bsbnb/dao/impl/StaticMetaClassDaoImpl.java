package kz.bsbnb.dao.impl;


import kz.bsbnb.dao.MetaClassDao;
import kz.bsbnb.usci.eav.model.meta.IMetaAttribute;
import kz.bsbnb.usci.eav.model.meta.IMetaType;
import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Primary
@Component
@Profile("dev")
public class StaticMetaClassDaoImpl implements MetaClassDao {

    protected MetaClass metaCredit;

    public StaticMetaClassDaoImpl() {
    }

    public StaticMetaClassDaoImpl(MetaClass metaCredit) {
        this.metaCredit = metaCredit;
    }

    @Override
    public MetaClass load(long id) {
        if(metaCredit.getId() == id)
            return metaCredit;

        for (String attribute : metaCredit.getAttributeNames()) {
            IMetaAttribute metaAttribute = metaCredit.getMetaAttribute(attribute);
            IMetaType metaType = metaAttribute.getMetaType();
            if(metaType.isComplex()) {
                if(!metaType.isSet()) {
                    MetaClass childMeta = (MetaClass) metaType;
                    if(childMeta.getId() == id)
                        return childMeta;
                }
            }
        }

        throw new RuntimeException("No such meta with id: " + id);
    }
}

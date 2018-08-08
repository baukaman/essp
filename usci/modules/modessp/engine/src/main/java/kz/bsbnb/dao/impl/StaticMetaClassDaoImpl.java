package kz.bsbnb.dao.impl;


import kz.bsbnb.dao.MetaClassDao;
import kz.bsbnb.usci.eav.model.meta.IMetaAttribute;
import kz.bsbnb.usci.eav.model.meta.IMetaType;
import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;
import kz.bsbnb.usci.eav.model.meta.impl.MetaSet;
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
        MetaClass ans = recursive(metaCredit, id);

        if(ans != null)
            return ans;

        throw new RuntimeException("No such meta with id: " + id);
    }

    private MetaClass recursive(MetaClass metaClass, long id){
        if(metaClass.getId() == id)
            return metaClass;

        for (String attribute : metaClass.getAttributeNames()) {
            IMetaAttribute metaAttribute = metaClass.getMetaAttribute(attribute);
            IMetaType metaType = metaAttribute.getMetaType();
            MetaClass ans = null;
            if(metaType.isComplex()) {
                if(!metaType.isSet()) {
                    MetaClass childMeta = (MetaClass) metaType;
                    ans = recursive(childMeta, id);
                } else {
                    MetaSet metaSet = (MetaSet) metaType;
                    MetaClass childType = ((MetaClass) metaSet.getMemberType());
                    ans = recursive(childType, id);
                }
            }

            if(ans != null)
                return ans;
        }

        return null;

    }
}

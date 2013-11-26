package kz.bsbnb.usci.core.service;

import kz.bsbnb.usci.eav.model.base.impl.BaseEntity;
import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;

import java.util.List;

/**
 * @author k.tulbassiyev
 */
public interface IEntityService {
    public BaseEntity load(long id);
    public void save(BaseEntity baseEntity);
    public BaseEntity search(BaseEntity baseEntity);
    public void update(BaseEntity baseEntitySave, BaseEntity baseEntityLoad);
    public List<Long> getEntityIDsByMetaclass(long id);
}

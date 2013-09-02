package kz.bsbnb.usci.eav.persistance.dao;

import kz.bsbnb.usci.eav.model.base.IBaseValue;
import kz.bsbnb.usci.eav.model.base.impl.BaseSet;
import kz.bsbnb.usci.eav.model.meta.impl.MetaSet;

/**
 *
 */
public interface IBeComplexSetValueDao {

    public long save(IBaseValue baseValue, MetaSet metaSet);

    public void remove(BaseSet baseSet);

}
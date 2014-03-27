package kz.bsbnb.usci.eav.persistance.dao;

import kz.bsbnb.usci.eav.model.base.IBaseValue;

/**
 * Created by Alexandr.Motov on 20.03.14.
 */
public interface IBeValueDao extends IPersistableDao {

    public IBaseValue getNextBaseValue(IBaseValue baseValue);

    public IBaseValue getPreviousBaseValue(IBaseValue baseValue);

    public IBaseValue getClosedBaseValue(IBaseValue baseValue);

    public IBaseValue getLastBaseValue(IBaseValue baseValue);

}

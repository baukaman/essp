package kz.bsbnb.usci.eav.model.base;


import kz.bsbnb.usci.eav.model.meta.IMetaAttribute;
import kz.bsbnb.usci.eav.model.meta.IMetaType;
import kz.bsbnb.usci.eav.model.persistable.IPersistable;

import java.util.Date;
import java.util.UUID;

public interface IBaseValue<T> extends IPersistable, Cloneable {
    IBaseContainer getBaseContainer();

    void setBaseContainer(IBaseContainer baseContainer);

    long getCreditorId();

    Date getCloseDate();

    void setCloseDate(Date closeDate);

    void setCreditorId(long creditorId);

    IMetaAttribute getMetaAttribute();

    void setMetaAttribute(IMetaAttribute metaAttribute);

    T getValue();

    void setValue(T value);

    Date getRepDate();

    void setRepDate(Date reportDate);

    void setLast(boolean last);

    boolean isLast();

    void setClosed(boolean closed);

    boolean isClosed();

    void setNewBaseValue(IBaseValue baseValue);

    IBaseValue getNewBaseValue();

    UUID getUuid();

    boolean equalsByValue(IBaseValue baseValue);

    boolean equalsByValue(IMetaType metaType, IBaseValue baseValue);

}

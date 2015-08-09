package kz.bsbnb.usci.eav.model.base;

import kz.bsbnb.usci.eav.model.base.impl.OperationType;
import kz.bsbnb.usci.eav.model.meta.IMetaAttribute;
import kz.bsbnb.usci.eav.model.meta.IMetaClass;
import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface IBaseEntity extends IBaseContainer {
    IMetaAttribute getMetaAttribute(String attribute);

    IBaseEntityReportDate getBaseEntityReportDate();

    void setBaseEntityReportDate(IBaseEntityReportDate baseEntityReportDate);

    Date getReportDate();

    void setReportDate(Date reportDate);

    MetaClass getMeta();

    void remove(String attribute);

    OperationType getOperation();

    Object getEl(String path);

    Object getEls(String path);

    List<Object> getElWithArrays(String path);

    IBaseValue safeGetValue(String name);

    void calculateValueCount();

    UUID getUuid();
}

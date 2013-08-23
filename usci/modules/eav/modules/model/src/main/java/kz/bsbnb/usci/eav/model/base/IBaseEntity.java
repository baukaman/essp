package kz.bsbnb.usci.eav.model.base;

import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;

import java.util.Date;
import java.util.Set;

/**
 * @author a.motov
 */
public interface IBaseEntity extends IBaseContainer {

    public IBaseValue getBaseValue(String attribute);

    public Date getReportDate();

    public void setReportDate(Date reportDate);

    public Set<Date> getAvailableReportDates();

    public MetaClass getMeta();

    public Set<String> getAttributeNames();

    public void remove(String attribute);

}

package kz.bsbnb.usci.eav.model.base.impl.value;

import kz.bsbnb.usci.eav.model.Batch;
import kz.bsbnb.usci.eav.model.base.IBaseValue;
import kz.bsbnb.usci.eav.model.base.impl.BaseValue;

import java.util.Date;

public class BaseEntityDateValue extends BaseValue<Date> implements IBaseValue<Date> {
    public BaseEntityDateValue(long id, long creditorId, Batch batch, long index, Date reportDate, Date value,
                               boolean closed, boolean last) {
        super(id, creditorId, batch, index, reportDate, value, closed, last);
    }

    public BaseEntityDateValue(long creditorId, Batch batch, long index, Date value) {
        super(creditorId, batch, index, value);
    }

}

package kz.bsbnb;

import java.sql.Timestamp;
import java.util.Date;

public class DataDateValue extends DataValue<Date> {

    public DataDateValue(Date value) {
        super(value);
    }

    public DataDateValue(Object value) {
        if(value instanceof Timestamp)
            this.value = new Date((((Timestamp) value).getTime()));
        else
            this.value = ((Date) value);
    }
}

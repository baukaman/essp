package kz.bsbnb.attribute;

import kz.bsbnb.DataValue;

public class SimpleEntityAttribute extends EntityAttribute {

    public SimpleEntityAttribute(String attribute, DataValue value) {
        super(attribute, value);
    }

    @Override
    public String getColumnName() {
        if(attribute.equals("date"))
            return "date_";
        return super.getColumnName();
    }
}

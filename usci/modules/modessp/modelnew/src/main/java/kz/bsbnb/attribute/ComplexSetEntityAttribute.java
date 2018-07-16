package kz.bsbnb.attribute;

import kz.bsbnb.DataValue;

public class ComplexSetEntityAttribute extends EntityAttribute {

    protected ComplexSetEntityAttribute(String attribute, DataValue value) {
        super(attribute, value);
    }

    @Override
    public Object getColumnValue() {
        throw new UnsupportedOperationException();
    }
}

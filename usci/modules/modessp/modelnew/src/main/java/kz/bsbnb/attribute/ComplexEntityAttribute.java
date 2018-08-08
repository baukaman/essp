package kz.bsbnb.attribute;

import kz.bsbnb.DataEntity;
import kz.bsbnb.DataValue;

import javax.sql.DataSource;

public class ComplexEntityAttribute extends EntityAttribute {
    DataEntity entity;

    public ComplexEntityAttribute(String attribute, DataValue value) {
        super(attribute, value);
        this.entity = ((DataEntity) value.getValue());
    }

    @Override
    public String getColumnName() {
        return attribute + "_ID";
    }

    @Override
    public Object getColumnValue(DataSource dataSource) {
        return entity.getId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComplexEntityAttribute)) return false;
        if(entity.getId() < 1)
            return false;
        return entity.getId() == ((ComplexEntityAttribute) o).entity.getId();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}

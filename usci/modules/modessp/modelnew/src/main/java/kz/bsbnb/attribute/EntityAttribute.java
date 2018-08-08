package kz.bsbnb.attribute;

import kz.bsbnb.DataValue;
import kz.bsbnb.usci.eav.model.meta.IMetaType;

import javax.sql.DataSource;

public class EntityAttribute {
    protected DataValue value;
    protected String attribute;

    protected EntityAttribute(String attribute, DataValue value) {
        this.attribute = attribute;
        this.value = value;

    }

    public String getColumnName() {
        return attribute;
    }

    public Object getColumnValue(DataSource dataSource) {
        return value.getValue();
    }

    public static class Builder {
        IMetaType metaType;
        DataValue value;
        String attribute;

        public Builder(String attribute, IMetaType metaType, DataValue value){
            this.attribute = attribute;
            this.metaType = metaType;
            this.value = value;
        }
        public EntityAttribute build(){
            if(metaType.isComplex()) {
                if(!metaType.isSet()) {
                    return new ComplexEntityAttribute(attribute, value);
                } else {
                    return new ComplexSetEntityAttribute(attribute, value);
                }
            } else {
                if (!metaType.isSet()) {
                    return new SimpleEntityAttribute(attribute, value);
                } else {
                    throw new UnsupportedOperationException();
                }
            }

        }
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;

        if(obj instanceof EntityAttribute) {
            return attribute.equalsIgnoreCase(((EntityAttribute) obj).attribute);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return attribute.hashCode();
    }
}

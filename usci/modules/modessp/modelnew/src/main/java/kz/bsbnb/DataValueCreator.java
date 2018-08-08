package kz.bsbnb;

import kz.bsbnb.usci.eav.model.type.DataTypes;

import java.util.Date;

public class DataValueCreator {
    public static DataValue getValue(DataTypes type, String value){
        switch (type) {
            case STRING:
                return new DataStringValue(value);
            case DATE:
                return new DataDateValue(((Date) DataTypes.getCastObject(type, value)));
            case DOUBLE:
                return new DataDoubleValue(((Double) DataTypes.getCastObject(type, value)));
            case BOOLEAN:
                return new DataBooleanValue(((Boolean) DataTypes.getCastObject(type, value)));
            default:
                throw new RuntimeException("no such type");
        }
    }
}

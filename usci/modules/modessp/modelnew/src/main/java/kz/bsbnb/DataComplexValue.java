package kz.bsbnb;

import javax.annotation.Nullable;

public class DataComplexValue extends DataValue<DataEntity> {
    public DataComplexValue(DataEntity entity) {
        super(entity);
    }

    @Override
    public boolean isOneRow(@Nullable DataValue baseValue) {
        if(baseValue == null)
            return false;

        return value.getId() == ((DataEntity) baseValue.getValue()).getId();
    }
}

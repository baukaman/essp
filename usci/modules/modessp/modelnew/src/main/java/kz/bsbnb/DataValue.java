package kz.bsbnb;

import kz.bsbnb.base.DataContainer;
import kz.bsbnb.usci.eav.model.meta.impl.MetaAttribute;

import javax.annotation.Nullable;
import java.util.Objects;

public class DataValue<T> {
    DataContainer dataContainer;
    MetaAttribute metaAttribute;
    DataValue newDataValue;
    T value;

    protected DataValue(){

    }

    public DataValue(T value) {
        if(value == null)
            throw new RuntimeException("cannot pass null to DataValue");

        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if(o instanceof DataValue) {
            return Objects.equals(this.value, ((DataValue) o).value);
        }

        return Objects.equals(this.value, o);
    }

    public T getValue() {
        return value;
    }

    public boolean isOneRow(@Nullable DataValue baseValue) {
        if(baseValue == null)
            return false;

        return value.equals(baseValue.getValue());
    }
}

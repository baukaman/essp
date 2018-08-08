package kz.bsbnb;

import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;

import java.util.HashSet;
import java.util.Set;

public class DataSet {
    MetaClass metaClass;
    Set<DataEntity> values;

    public DataSet(MetaClass metaClass) {
        this.metaClass = metaClass;
        values = new HashSet<>();
    }

    public MetaClass getMetaClass() {
        return metaClass;
    }

    public void add(DataEntity entity) {
        values.add(entity);
    }

    public Set<DataEntity> getValues() {
        return values;
    }
}

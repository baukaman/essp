package kz.bsbnb;

import kz.bsbnb.attribute.EntityAttribute;
import kz.bsbnb.usci.eav.model.meta.IMetaAttribute;
import kz.bsbnb.usci.eav.model.meta.IMetaType;
import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;

import java.util.*;

public final class DataEntity {
    private long id;
    private long creditorId;
    private Date reportDate;
    DataOperationType dataOperation;
    MetaClass metaClass;
    Map<String, DataValue> values;

    public DataEntity(MetaClass metaCredit) {
        this.metaClass = metaCredit;
        values = new HashMap<>();
    }

    public void setDataValue(String attribute, DataValue dataValue) {
        IMetaAttribute metaAttribute = metaClass.getMetaAttribute(attribute);
        if(metaAttribute == null)
            throw new RuntimeException("no such attribute: " + attribute);

        values.put(attribute, dataValue);
    }

    public Iterator<EntityAttribute> getEntityIterator() {
        return new Iter();
    }

    public boolean equalsByKey(DataEntity entity) {
        String[] keyFields = new String[]{"code", "short_name"};
        for (String keyField : keyFields) {
            if(getEl(keyField) != null && entity.getEl(keyField) != null)
                return Objects.equals(getEl(keyField), entity.getEl(keyField));
        }

        return false;
    }

    private class Iter implements Iterator<EntityAttribute> {
        protected Iterator<String> iterator;

        private Iter(){
            iterator = values.keySet().iterator();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public EntityAttribute next() {
            String attribute = iterator.next();
            IMetaAttribute metaAttribute = metaClass.getMetaAttribute(attribute);
            DataValue dataValue = getBaseValue(attribute);
            IMetaType metaType = metaAttribute.getMetaType();
            return new EntityAttribute.Builder(attribute, metaType, dataValue).build();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();

        }
    }

    public Object getEl(String path) {
        DataEntity ret = this;
        String[] array = path.split("\\.");
        for (int i = 0; i < array.length; i++) {
            DataValue dataValue = ret.values.get(array[i]);
            if (dataValue == null) {
                return null;
            } else if(i < array.length - 1)
                ret = ((DataEntity) dataValue.getValue());
            else
                return dataValue.getValue();
        }

        return ret;
    }

    public List<DataEntity> getEls(String path){
        List<DataEntity> ret = new LinkedList<>();
        ret.add(this);
        String[] array = path.split("\\.");
        for(String attribute: array) {
            List<DataEntity> nextArray = new LinkedList<>();
            for (DataEntity entity : ret) {
                IMetaAttribute metaAttribute = metaClass.getMetaAttribute(attribute);
                IMetaType metaType = metaAttribute.getMetaType();
                if(metaType.isComplex()) {
                    DataValue dataValue = entity.values.get(attribute);
                    if(metaType.isSet()) {
                        DataSet set = (DataSet) dataValue.getValue();
                        for (DataEntity value : set.values) {
                            nextArray.add(value);
                        }
                    } else {
                        DataEntity childEntity = ((DataEntity) dataValue.getValue());
                        nextArray.add(childEntity);
                    }
                }
            }
            ret = nextArray;
        }

        return ret;
    }

    public MetaClass getMeta() {
        return metaClass;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public Set<String> getAttributes(){
        return values.keySet();
    }

    public long getCreditorId() {
        return creditorId;
    }

    public Date getReportDate() {
        return reportDate;
    }

    public DataValue getBaseValue(String next) {
        return values.get(next);
    }

    public void setCreditorId(long creditorId) {
        this.creditorId = creditorId;
    }

    public void setReportDate(Date reportDate) {
        this.reportDate = reportDate;
    }

    public DataEntity withReportDate(Date reportDate) {
        this.reportDate = reportDate;
        return this;
    }

    public DataEntity clone() {
        DataEntity ret = new DataEntity(getMeta());
        ret.setId(getId());
        ret.setReportDate(getReportDate());
        ret.setCreditorId(getCreditorId());
        ret.values = new HashMap<>(values);
        return ret;
    }

    public boolean subsetOf(DataEntity loadedEntity) {
        for (String attribute : values.keySet()) {
            DataValue baseValue = getBaseValue(attribute);
            if(!baseValue.isOneRow(loadedEntity.getBaseValue(attribute)))
                return false;
        }

        return true;
    }

}

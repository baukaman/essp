package kz.bsbnb;

import kz.bsbnb.attribute.EntityAttribute;
import kz.bsbnb.usci.eav.model.meta.IMetaAttribute;
import kz.bsbnb.usci.eav.model.meta.IMetaType;
import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;
import kz.bsbnb.usci.eav.util.Errors;

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
            DataValue dataValue = getDataValue(attribute);
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

    public Object getEls(String path){
        Queue<Object> queue = new LinkedList<>();

        StringBuilder str = new StringBuilder();
        String[] operations = new String[500];
        boolean[] isFilter = new boolean[500];
        String function = null;

        if (!path.startsWith("{")) throw new RuntimeException(Errors.compose(Errors.E14));
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == '}') {
                function = path.substring(1, i);
                path = path.substring(i + 1);
                break;
            }
        }

        if (function == null) throw new RuntimeException(Errors.compose(Errors.E15));

        /*Set<Object> allowedSet = new TreeSet<>();

        if (function.startsWith("set")) {
            String[] elems = function.substring(function.indexOf('(') + 1, function.indexOf(')')).split(",");
            if (function.startsWith("setInt")) {
                allowedSet = new TreeSet<>();
                for (String e : elems)
                    allowedSet.add(Integer.parseInt(e.trim()));
            } else if (function.startsWith("setLong")) {
                allowedSet = new TreeSet<>();
                for (String e : elems)
                    allowedSet.add(Long.parseLong(e.trim()));
            } else if (function.startsWith("setString")) {
                allowedSet = new TreeSet<>();
                for (String e : elems)
                    allowedSet.add(e.trim());
            }
        }*/




        int yk = 0;
        int open = 0;
        int eqCnt = 0;

        for (int i = 0; i <= path.length(); i++) {
            if (i == path.length()) {
                if (open != 0)
                    throw new RuntimeException(Errors.compose(Errors.E18));
                break;
            }
            if (path.charAt(i) == '=') eqCnt++;
            if (path.charAt(i) == '!' && (i + 1 == path.length() || path.charAt(i + 1) != '='))
                throw new RuntimeException(Errors.compose(Errors.E21));

            if (path.charAt(i) == '[') open++;
            if (path.charAt(i) == ']') {
                open--;
                if (eqCnt != 1) throw new RuntimeException(Errors.compose(Errors.E20));
                eqCnt = 0;
            }
            if (open < 0 || open > 1) throw new RuntimeException(Errors.compose(Errors.E22));
        }

        for (int i = 0; i <= path.length(); i++) {
            if (i == path.length()) {
                if (str.length() > 0) {
                    String[] arr = str.toString().split("\\.");
                    for (String anArr : arr) {
                        operations[yk] = anArr;
                        isFilter[yk] = false;
                        yk++;
                    }
                }
                break;
            }
            char c = path.charAt(i);
            if (c == '[' || c == ']') {
                if (str.length() > 0) {
                    if (c == ']') {
                        operations[yk] = str.toString();
                        isFilter[yk] = true;
                        yk++;
                    } else {
                        String[] arr = str.toString().split("\\.");
                        for (String anArr : arr) {
                            operations[yk] = anArr;
                            isFilter[yk] = false;
                            yk++;
                        }
                    }
                    str.setLength(0);
                }
            } else {
                str.append(c);
            }
        }

        List<Object> ret = new LinkedList<>();
        queue.add(this);
        queue.add(0);
        int retCount = 0;

        while (queue.size() > 0) {
            Object curO = queue.poll();
            int step = (Integer) queue.poll();

            if (curO == null)
                continue;

            if (step == yk) {
                if (function.startsWith("count")) {
                    retCount++;
                }
                ret.add(curO);
                continue;
            }

            //noinspection ConstantConditions
            DataEntity curBE = (DataEntity) curO;
            MetaClass curMeta = curBE.getMeta();

            if (!isFilter[step]) {
                IMetaAttribute nextAttribute = curMeta.getMetaAttribute(operations[step]);

                if (!nextAttribute.getMetaType().isComplex()) { // transition to BASIC type
                    queue.add(curBE.getEl(operations[step]));
                    queue.add(step + 1);
                } else if (nextAttribute.getMetaType().isSet()) { //transition to array
                    DataSet next = (DataSet) curBE.getEl(operations[step]);
                    if (next != null) {
                        for (Object o : next.getValues()) {
                            {
                                //queue.add(((DataValue) o).getValue());
                                queue.add(o);
                                queue.add(step + 1);
                            }
                        }
                    }
                } else { //transition to simple
                    DataEntity next = (DataEntity) curBE.getEl(operations[step]);
                    queue.add(next);
                    queue.add(step + 1);
                }
            } else {
                String[] parts;
                boolean inv = false;

                if (operations[step].contains("!")) {
                    parts = operations[step].split("!=");
                    inv = true;
                } else
                    parts = operations[step].split("=");

                Object o = curBE.getEl(parts[0]);

                boolean expr = (o == null && parts[1].equals("null")) || (o != null && o.toString().equals(parts[1]));
                if (inv) expr = !expr;

                if (expr) {
                    queue.add(curO);
                    queue.add(step + 1);
                }
            }
        }

        if (function.startsWith("get"))
            return ret;

        return retCount;
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

    public DataValue getDataValue(String next) {
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
            DataValue DataValue = getDataValue(attribute);
            if(!DataValue.isOneRow(loadedEntity.getDataValue(attribute)))
                return false;
        }

        return true;
    }

}

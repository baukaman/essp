package kz.bsbnb.attribute;

import kz.bsbnb.DataEntity;
import kz.bsbnb.DataSet;
import kz.bsbnb.DataValue;
import oracle.jdbc.driver.OracleConnection;
import org.apache.commons.dbcp.DelegatingConnection;

import javax.sql.DataSource;
import java.sql.Array;
import java.util.HashSet;
import java.util.Set;

public class ComplexSetEntityAttribute extends EntityAttribute {

    protected ComplexSetEntityAttribute(String attribute, DataValue value) {
        super(attribute, value);
    }

    @Override
    public Object getColumnValue(DataSource dataSource) {
        Array tnumber = null;
        try {
            OracleConnection connection = ((OracleConnection) ((DelegatingConnection) dataSource.getConnection()).getInnermostDelegate());
            DataSet set = (DataSet) this.value.getValue();
            Set<DataEntity> values = set.getValues();
            Set<Long> ids = new HashSet<>();
            for (DataEntity dataEntity : values) {
                ids.add(dataEntity.getId());
            }

            tnumber = connection.createARRAY("TNUMBER", ids.toArray());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return tnumber;

        //throw new UnsupportedOperationException();
        //return new TNumber();
    }

    @Override
    public String getColumnName() {
        return attribute + "_ids";
        //return "docs_ids";
    }
}

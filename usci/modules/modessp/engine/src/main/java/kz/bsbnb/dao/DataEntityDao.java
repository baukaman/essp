package kz.bsbnb.dao;

import com.google.common.base.Optional;
import kz.bsbnb.*;
import kz.bsbnb.dao.base.BaseDao;
import kz.bsbnb.engine.SavingInfo;
import kz.bsbnb.usci.eav.model.meta.IMetaAttribute;
import kz.bsbnb.usci.eav.model.meta.IMetaType;
import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;
import kz.bsbnb.usci.eav.model.meta.impl.MetaValue;
import kz.bsbnb.usci.eav.util.DataUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.*;

@Component
//@Scope(value = "thread")
public class DataEntityDao extends BaseDao {


    @Autowired
    SavingInfo savingInfo;

    /**
     * 1) sequence generation technique ???
     * 2) fk -> eav_be_entities
     *
     * @param entity
     */
    public void insert(DataEntity entity) {
        insertEBE(entity);
        MetaClass meta = entity.getMeta();
        StringBuilder buf = new StringBuilder(100);
        buf.append("insert into ")
                .append(meta.getClassName());
        buf.append(" (");
        buf.append("creditor_id,");
        buf.append("report_date,");
        buf.append("entity_id,");
        Iterator<String> it = entity.getAttributes().iterator();
        while(it.hasNext()) {
            String attribute = it.next();
            IMetaAttribute metaAttribute = meta.getMetaAttribute(attribute);
            IMetaType metaType = metaAttribute.getMetaType();
            if(metaType.isComplex()) {
                buf.append(attribute + "_ID");
            } else {
                buf.append(safeColumnName(attribute));
            }
            if(it.hasNext())
                buf.append(",");
        }
        buf.append(") values (?,?,?,");

        it = entity.getAttributes().iterator();
        Object[] values = new Object[entity.getAttributes().size() + 3];
        int i = 0;
        values[i++] = savingInfo.getCreditorId();
        values[i++] = savingInfo.getReportDate();
        values[i++] = entity.getId();
        while(it.hasNext()) {
            String attribute = it.next();
            IMetaAttribute metaAttribute = meta.getMetaAttribute(attribute);
            IMetaType metaType = metaAttribute.getMetaType();
            Object value = entity.getBaseValue(attribute).getValue();
            if(metaType.isComplex()) {
                values[i++] = ((DataEntity) value).getId();
            } else {
                values[i++] = value;
            }
            buf.append("?");
            if(it.hasNext())
                buf.append(",");
        }
        buf.append(")");

        System.out.println(buf.toString());

        jdbcTemplate.update(buf.toString(),values);
        databaseActivity.insert();

    }

    private void insertEBE(DataEntity entity) {
        MetaClass meta = entity.getMeta();
        /*StringBuilder buf = new StringBuilder(100);
        buf.append("insert into ")
                .append("EAV_BE_ENTITIES");
        buf.append(" (");
        buf.append("creditor_id,");
        buf.append("entity_id,");
        buf.append("class_id,");
        buf.append("entity_key");
        //buf.append(") values (?,SEQ_ENTITY.nexvtal,?) returning entity_id");
        buf.append(") values (?,2,?,?) returning into entity_id");

        /*jdbcTemplate.update(buf.toString(),
                        new Object[]{entity.getCreditorId(),
                        meta.getId(),""});*/
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate);
        simpleJdbcInsert.withTableName("EAV_BE_ENTITIES")
                .usingGeneratedKeyColumns("ENTITY_ID");
        Map<String,Object> parameters = new HashMap<>();
        parameters.put("creditor_id", entity.getCreditorId());
        //parameters.put("entity_id", 1L);
        parameters.put("class_id", meta.getId());
        parameters.put("entity_key"," ");
        parameters.put("IS_DELETED","0");
        parameters.put("SYSTEM_DATE", new Date());
        Number number = simpleJdbcInsert.executeAndReturnKey(new MapSqlParameterSource(parameters));
        databaseActivity.insert();
        System.out.println(number.longValue());
        entity.setId(number.longValue());
    }


    public DataEntity load(long id, long creditorId, Date reportDate) {
        List<Map<String, Object>> maps = jdbcTemplate.queryForList("SELECT * FROM EAV_BE_ENTITIES where ENTITY_ID = :ENTITY_ID", id);
        databaseActivity.select();

        if(maps.size() < 1)
            throw new RuntimeException("No such entity with id:" + id);

        if(maps.size() != 1)
            throw new RuntimeException("Incorrect fetch size: " + id);

        Map<String, Object> row = maps.iterator().next();
        long classId = ((BigDecimal) row.get("CLASS_ID")).longValue();
        MetaClass metaClass = metaClassDao.load(classId);
        DataEntity entity = new DataEntity(metaClass)
                .withReportDate(reportDate);
        entity.setId(id);
        entity.setCreditorId(creditorId);
        entity.setReportDate(reportDate);
        StringBuilder buf = new StringBuilder(100);
        buf.append("SELECT * FROM ");
        buf.append(metaClass.getClassName());
        buf.append(" WHERE ENTITY_ID = ?");
        buf.append(" AND CREDITOR_ID = ?");
        buf.append(" AND REPORT_DATE = ?");

        maps = jdbcTemplate.queryForList(buf.toString(), id, creditorId, reportDate);
        databaseActivity.select();
        assert maps.size() == 1;

        Map<String, Object> next = maps.iterator().next();
        for (String attribute : metaClass.getAttributeNames()) {
            if(next.get(attribute) != null) {
                IMetaAttribute metaAttribute = metaClass.getMetaAttribute(attribute);
                IMetaType metaType = metaAttribute.getMetaType();
                if(!metaType.isComplex()) {
                    if(!metaType.isSet()) {
                        MetaValue metaSimple = (MetaValue) metaType;
                        Object value = next.get(attribute);
                        switch (metaSimple.getTypeCode()) {
                            case DOUBLE:
                                entity.setDataValue(attribute, new DataDoubleValue(value));
                                break;
                            case STRING:
                                entity.setDataValue(attribute, new DataStringValue(value));
                                break;
                            case DATE:
                                entity.setDataValue(attribute, new DataDateValue(value));
                                break;

                        }

                    }
                }
            }
        }
        return entity;
    }

    public Optional<DataEntity> loadByMaxReportDate(DataEntity entity) {
        StringBuilder buf = new StringBuilder(100);
        MetaClass metaClass = entity.getMeta();
        buf.append("SELECT MAX(REPORT_DATE) FROM ");
        buf.append(metaClass.getClassName());
        buf.append(" WHERE ENTITY_ID = ?");
        buf.append(" AND CREDITOR_ID = ?");
        buf.append(" AND REPORT_DATE <= ?");


        Date date = jdbcTemplate.queryForObject(buf.toString(), Date.class, entity.getId(), entity.getCreditorId(), entity.getReportDate());
        databaseActivity.select();
        if(date == null)
            return Optional.absent();

        //date must not be Timestamp, fails in tests
        return Optional.of(load(entity.getId(), entity.getCreditorId(), DataUtils.convert(date)));
    }
}

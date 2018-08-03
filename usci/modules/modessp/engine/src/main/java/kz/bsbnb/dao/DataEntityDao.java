package kz.bsbnb.dao;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import kz.bsbnb.*;
import kz.bsbnb.attribute.EntityAttribute;
import kz.bsbnb.dao.base.BaseDao;
import kz.bsbnb.SavingInfo;
import kz.bsbnb.dao.base.RDLoadType;
import kz.bsbnb.engine.IRefEngine;
import kz.bsbnb.exception.RefLoadException;
import kz.bsbnb.usci.eav.model.meta.IMetaAttribute;
import kz.bsbnb.usci.eav.model.meta.IMetaType;
import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;
import kz.bsbnb.usci.eav.model.meta.impl.MetaValue;
import kz.bsbnb.usci.eav.util.DataUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
//@Scope(value = "thread")
public class DataEntityDao extends BaseDao {


    @Autowired
    SavingInfo savingInfo;

    @Autowired
    IRefEngine refEngine;

    /**
     * 1) sequence generation technique ???
     * 2) fk -> eav_be_entities
     *
     * @param entity
     */
    public void insertNewEntity(DataEntity entity) {
        insertEBE(entity);
        insert(entity);
    }

    public void insert(DataEntity entity) {
        MetaClass meta = entity.getMeta();
        StringBuilder buf = new StringBuilder(100);
        List values = new LinkedList();
        buf.append("insert into ")
                .append(meta.getClassName());
        buf.append(" (");
        buf.append("creditor_id,");
        values.add(savingInfo.getCreditorId());
        buf.append("report_date,");
        values.add(savingInfo.getReportDate());
        buf.append("entity_id,");
        values.add(entity.getId());
        Iterator<EntityAttribute> entityIterator = entity.getEntityIterator();
        while(entityIterator.hasNext()) {
            EntityAttribute attribute = entityIterator.next();
            buf.append(attribute.getColumnName());
            values.add(attribute.getColumnValue());
            if(entityIterator.hasNext())
                buf.append(",");
        }

        buf.append(") values (" + Strings.repeat("?, ", values.size() - 1) + "?)");
        System.out.println(buf.toString());

        jdbcTemplate.update(buf.toString(),values.toArray());
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


    public DataEntity load(long id, long creditorId, Date reportDate) throws RefLoadException {
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
            IMetaAttribute metaAttribute = metaClass.getMetaAttribute(attribute);
            IMetaType metaType = metaAttribute.getMetaType();

            if(next.get(safeColumnName(attribute)) != null) {
                if(!metaType.isComplex()) {
                    if(!metaType.isSet()) {
                        MetaValue metaSimple = (MetaValue) metaType;
                        Object value = next.get(safeColumnName(attribute));
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
            //complex values
            } else if (next.get(attribute + "_ID") != null) {
                //collision with column
                if(attribute.equalsIgnoreCase("creditor"))
                    continue;
                Long childId = ((BigDecimal) next.get(attribute + "_ID")).longValue();
                if(metaType.isReference()) {
                    entity.setDataValue(attribute, new DataComplexValue(refEngine.getById(childId, reportDate)));
                } else {
                    entity.setDataValue(attribute, new DataComplexValue(load(childId, creditorId, reportDate)));
                }
            }
        }
        return entity;
    }

    private Optional<DataEntity> loadByRd(DataEntity entity, RDLoadType loadType) throws RefLoadException {
        StringBuilder buf = new StringBuilder(100);
        MetaClass metaClass = entity.getMeta();
        buf.append("SELECT "+loadType.agr+"(REPORT_DATE) FROM ");
        buf.append(metaClass.getClassName());
        buf.append(" WHERE ENTITY_ID = ?");
        buf.append(" AND CREDITOR_ID = ?");
        buf.append(" AND REPORT_DATE "+ loadType.sign + " ?");


        Date date = jdbcTemplate.queryForObject(buf.toString(), Date.class, entity.getId(), entity.getCreditorId(), entity.getReportDate());
        databaseActivity.select();
        if(date == null)
            return Optional.absent();

        //date must not be Timestamp, fails in tests
        return Optional.of(load(entity.getId(), entity.getCreditorId(), DataUtils.convert(date)));
    }

    public void update(DataEntity entity) {
        StringBuilder buf = new StringBuilder(100);
        MetaClass metaClass = entity.getMeta();
        buf.append("update " + metaClass.getClassName() + " set ");
        List<Object> list = new LinkedList<>();

        Iterator<EntityAttribute> iterator = entity.getEntityIterator();
        while(iterator.hasNext()) {
            EntityAttribute entityAttribute = iterator.next();
            buf.append(entityAttribute.getColumnName() + " = ?");
            list.add(entityAttribute.getColumnValue());
            if(iterator.hasNext())
                buf.append(",");
        }

        buf.append(" where");
        buf.append(" entity_id = ?");
        list.add(entity.getId());

        buf.append(" and creditor_id = ?");
        list.add(savingInfo.getCreditorId());

        buf.append(" and report_date = ?");
        list.add(savingInfo.getReportDate());

        int update = jdbcTemplate.update(buf.toString(), list.toArray());
        System.out.println(buf.toString());
        databaseActivity.update();
        assert update == 1;
    }

    public Optional<DataEntity> loadByMaxReportDate(DataEntity entity) throws RefLoadException {
        return loadByRd(entity, RDLoadType.BYMAX);
    }

    public Optional<DataEntity> loadByMinReportDate(DataEntity entity) throws RefLoadException {
        return loadByRd(entity, RDLoadType.BYMIN);
    }

    public void updateReportDate(DataEntity entity, Date reportDate) {
        StringBuilder buf = new StringBuilder(50);
        buf.append("update ").append(entity.getMeta().getClassName());
        buf.append(" set report_date = ? where entity_id = ? and creditor_id = ? and report_date = ?");
        System.out.println(buf.toString());
        jdbcTemplate.update(buf.toString(), reportDate, entity.getId(), savingInfo.getCreditorId(), entity.getReportDate());
        databaseActivity.update();
    }
}

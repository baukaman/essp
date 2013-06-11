package kz.bsbnb.usci.eav.postgresql.dao;

import kz.bsbnb.usci.eav.model.base.ContainerTypes;
import kz.bsbnb.usci.eav.model.meta.IMetaAttribute;
import kz.bsbnb.usci.eav.model.meta.IMetaContainer;
import kz.bsbnb.usci.eav.model.meta.IMetaType;
import kz.bsbnb.usci.eav.model.meta.impl.MetaAttribute;
import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;
import kz.bsbnb.usci.eav.model.meta.impl.MetaSet;
import kz.bsbnb.usci.eav.model.meta.impl.MetaValue;
import kz.bsbnb.usci.eav.model.type.ComplexKeyTypes;
import kz.bsbnb.usci.eav.model.type.DataTypes;
import kz.bsbnb.usci.eav.persistance.dao.IMetaClassDao;
import kz.bsbnb.usci.eav.persistance.impl.db.JDBCSupport;
import kz.bsbnb.usci.eav.util.SetUtils;
import org.jooq.DeleteConditionStep;
import org.jooq.InsertOnDuplicateStep;
import org.jooq.SelectForUpdateStep;
import org.jooq.UpdateConditionStep;
import org.jooq.impl.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static kz.bsbnb.eav.persistance.generated.Tables.*;

//TODO: refactor all id casts!
//TODO: refactor all field names. Get field names from generated classes.

@Repository
public class PostgreSQLMetaClassDaoImpl extends JDBCSupport implements IMetaClassDao {
	private final Logger logger = LoggerFactory.getLogger(PostgreSQLMetaClassDaoImpl.class);

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    private Executor sqlGenerator;

    private void loadAllClasses(List<MetaClass> metaClassList){
        SelectForUpdateStep select;



                select = sqlGenerator.select(
                        EAV_M_CLASSES.IS_DISABLED,
                        EAV_M_CLASSES.BEGIN_DATE,
                        EAV_M_CLASSES.ID,
                        EAV_M_CLASSES.NAME,
                        EAV_M_CLASSES.COMPLEX_KEY_TYPE,
                        EAV_M_CLASSES.IS_IMMUTABLE,
                        EAV_M_CLASSES.IS_REFERENCE
                ).from(EAV_M_CLASSES).
                  orderBy(EAV_M_CLASSES.BEGIN_DATE.desc());


        logger.debug(select.toString());
        List<Map<String, Object>> rows = queryForListWithStats(select.getSQL(), select.getBindValues().toArray());


        if (rows.size() < 1)
            throw new IllegalArgumentException("Classes not found.");

        for (Map<String, Object> row : rows){

            MetaClass metaClass = new MetaClass();

                metaClass.setDisabled((Boolean)row.get("is_disabled"));
                metaClass.setBeginDate((Timestamp)row.get("begin_date"));
                metaClass.setId((Long)row.get("id"));
                metaClass.setClassName((String)row.get("name"));
                metaClass.setComplexKeyType(ComplexKeyTypes.valueOf((String)row.get("complex_key_type")));
                metaClass.setImmutable((Boolean)row.get("is_immutable"));
                metaClass.setReference((Boolean)row.get("is_reference"));
                loadAttributes(metaClass);
            metaClassList.add(metaClass);
        }
    }

    public List<MetaClass> loadAll(){

        List<MetaClass> metaClassList = new ArrayList<MetaClass>();
        loadAllClasses(metaClassList);

      return metaClassList;
    }

    private void loadClass(MetaClass metaClass, boolean beginDateStrict)
    {
        SelectForUpdateStep select;

        if(metaClass.getId() < 1)
        {
            if(metaClass.getClassName() == null)
                throw new IllegalArgumentException("Meta class does not have name or id. Can't load.");

            if(beginDateStrict)
            {
                select = sqlGenerator.select(
                        EAV_M_CLASSES.IS_DISABLED,
                        EAV_M_CLASSES.BEGIN_DATE,
                        EAV_M_CLASSES.ID,
                        EAV_M_CLASSES.NAME,
                        EAV_M_CLASSES.COMPLEX_KEY_TYPE,
                        EAV_M_CLASSES.IS_IMMUTABLE,
                        EAV_M_CLASSES.IS_REFERENCE
                    ).from(EAV_M_CLASSES).
                    where(
                            EAV_M_CLASSES.NAME.equal(metaClass.getClassName())
                    ).and(
                        EAV_M_CLASSES.BEGIN_DATE.eq(metaClass.getBeginDate())
                    ).and(
                        EAV_M_CLASSES.IS_DISABLED.equal(false)
                    ).orderBy(EAV_M_CLASSES.BEGIN_DATE.desc()).limit(1).offset(0);
            }
            else
            {
                select = sqlGenerator.select(
                        EAV_M_CLASSES.IS_DISABLED,
                        EAV_M_CLASSES.BEGIN_DATE,
                        EAV_M_CLASSES.ID,
                        EAV_M_CLASSES.NAME,
                        EAV_M_CLASSES.COMPLEX_KEY_TYPE,
                        EAV_M_CLASSES.IS_IMMUTABLE,
                        EAV_M_CLASSES.IS_REFERENCE
                    ).from(EAV_M_CLASSES).
                    where(
                        EAV_M_CLASSES.NAME.equal(metaClass.getClassName())
                    ).and(
                        EAV_M_CLASSES.BEGIN_DATE.le(metaClass.getBeginDate())
                    ).and(
                        EAV_M_CLASSES.IS_DISABLED.equal(false)
                    ).orderBy(EAV_M_CLASSES.BEGIN_DATE.desc()).limit(1).offset(0);
            }

        }
        else
        {
            select = sqlGenerator.select(
                    EAV_M_CLASSES.IS_DISABLED,
                    EAV_M_CLASSES.BEGIN_DATE,
                    EAV_M_CLASSES.ID,
                    EAV_M_CLASSES.NAME,
                    EAV_M_CLASSES.COMPLEX_KEY_TYPE,
                    EAV_M_CLASSES.IS_IMMUTABLE,
                    EAV_M_CLASSES.IS_REFERENCE
                ).from(EAV_M_CLASSES).
                where(
                    EAV_M_CLASSES.ID.equal(metaClass.getId())
                ).limit(1).offset(0);
        }

        logger.debug(select.toString());
        List<Map<String, Object>> rows = queryForListWithStats(select.getSQL(), select.getBindValues().toArray());

        if (rows.size() > 1)
            throw new IllegalArgumentException("More then one class found. Can't load class : "
                    + metaClass.getClassName());

        if (rows.size() < 1)
            throw new IllegalArgumentException("Class not found. Can't load class : "
                    + metaClass.getClassName());

        Map<String, Object> row = rows.get(0);

        if(row != null) {
            metaClass.setDisabled((Boolean)row.get("is_disabled"));
            metaClass.setBeginDate((Timestamp)row.get("begin_date"));
            metaClass.setId((Long)row.get("id"));
            metaClass.setClassName((String)row.get("name"));
            metaClass.setComplexKeyType(ComplexKeyTypes.valueOf((String)row.get("complex_key_type")));
            metaClass.setImmutable((Boolean)row.get("is_immutable"));
            metaClass.setReference((Boolean)row.get("is_reference"));
        } else {
            logger.error("Can't load metaClass, empty data set.");
        }
    }
	
	private long createClass(MetaClass metaClass)
    {
        try
        {
            InsertOnDuplicateStep insert = sqlGenerator.insertInto(
                    EAV_M_CLASSES,
                    EAV_M_CLASSES.NAME,
                    EAV_M_CLASSES.COMPLEX_KEY_TYPE,
                    EAV_M_CLASSES.BEGIN_DATE,
                    EAV_M_CLASSES.IS_DISABLED,
                    EAV_M_CLASSES.IS_IMMUTABLE,
                    EAV_M_CLASSES.IS_REFERENCE
                ).values(metaClass.getClassName(),
                    metaClass.getComplexKeyType().toString(),
                    metaClass.getBeginDate(),
                    metaClass.isDisabled(),
                    metaClass.isImmutable(),
                    metaClass.isReference());

            logger.debug(insert.toString());
            long metaId = insertWithId(insert.getSQL(), insert.getBindValues().toArray());

            if(metaId < 1)
            {
                logger.error("Can't create class");
                return 0;
            }

            metaClass.setId(metaId);

            return metaId;
        }
        catch (DuplicateKeyException e)
        {
            logger.error("Duplicate name for class: " + metaClass.getClassName());
            throw new IllegalArgumentException("Duplicate name for class: " + metaClass.getClassName());
        }
    }

    private void updateClass(MetaClass metaClass)
    {
        if(metaClass.getId() < 1)
        {
            throw new IllegalArgumentException("MetaClass must have id to be updated");
        }

        UpdateConditionStep update = sqlGenerator.update(EAV_M_CLASSES
            ).set(EAV_M_CLASSES.NAME, metaClass.getClassName()
            ).set(EAV_M_CLASSES.COMPLEX_KEY_TYPE, metaClass.getComplexKeyType().toString()
            ).set(EAV_M_CLASSES.BEGIN_DATE, metaClass.getBeginDate()
            ).set(EAV_M_CLASSES.IS_DISABLED, metaClass.isDisabled()
            ).set(EAV_M_CLASSES.IS_REFERENCE, metaClass.isReference()
            ).set(EAV_M_CLASSES.IS_IMMUTABLE, metaClass.isImmutable()
            ).where(EAV_M_CLASSES.ID.eq(metaClass.getId()));

        logger.debug(update.toString());

        long t = 0;
        if(sqlStats != null)
        {
            t = System.nanoTime();
        }
        jdbcTemplate.update(update.getSQL(), update.getBindValues().toArray());
        if(sqlStats != null)
        {
            sqlStats.put(update.getSQL(), (System.nanoTime() - t) / 1000000);
        }
    }

    private long saveSet(IMetaType type, long parentId, int parentType, IMetaAttribute metaAttribute, String attributeName)
    {
        InsertOnDuplicateStep insert;
        long id;

        if(!type.isSet())
        {
            throw new IllegalStateException(attributeName + " is not an array.");
        }

        MetaSet metaSet = (MetaSet)type;

        if(metaSet.getMemberType().isSet())
        {
            insert = sqlGenerator.insertInto(
                    EAV_M_SET_OF_SETS,
                    EAV_M_SET_OF_SETS.CONTAINING_ID,
                    EAV_M_SET_OF_SETS.CONTAINER_TYPE,
                    EAV_M_SET_OF_SETS.NAME,
                    EAV_M_SET_OF_SETS.IS_KEY,
                    EAV_M_SET_OF_SETS.IS_NULLABLE,
                    EAV_M_SET_OF_SETS.ARRAY_KEY_TYPE,
                    EAV_M_SET_OF_SETS.IS_IMMUTABLE,
                    EAV_M_SET_OF_SETS.IS_REFERENCE
                ).values(parentId, parentType, attributeName,
                    metaAttribute.isKey(), metaAttribute.isNullable(),
                    metaSet.getArrayKeyType().toString(),
                    metaSet.isImmutable(), metaSet.isReference());

            logger.debug(insert.toString());

            long t = 0;
            if(sqlStats != null)
            {
                t = System.nanoTime();
            }

            id = insertWithId(insert.getSQL(), insert.getBindValues().toArray());
            metaSet.setId(id);

            if(sqlStats != null)
            {
                sqlStats.put(insert.getSQL(), (System.nanoTime() - t) / 1000000);
            }

            saveSet(metaSet.getMemberType(), id, ContainerTypes.SET, new MetaAttribute(false, false, null), "item");
        }
        else
        {
            if(metaSet.isComplex())
            {
                long innerId = save((MetaClass)metaSet.getMemberType());

                insert = sqlGenerator.insertInto(
                        EAV_M_COMPLEX_SET,
                        EAV_M_COMPLEX_SET.CONTAINING_ID,
                        EAV_M_COMPLEX_SET.CONTAINER_TYPE,
                        EAV_M_COMPLEX_SET.NAME,
                        EAV_M_COMPLEX_SET.IS_KEY,
                        EAV_M_COMPLEX_SET.IS_NULLABLE,
                        EAV_M_COMPLEX_SET.CLASS_ID,
                        EAV_M_COMPLEX_SET.ARRAY_KEY_TYPE,
                        EAV_M_COMPLEX_SET.IS_IMMUTABLE,
                        EAV_M_COMPLEX_SET.IS_REFERENCE
                    ).values(parentId, parentType, attributeName, metaAttribute.isKey(), metaAttribute.isNullable(),
                        innerId, metaSet.getArrayKeyType().toString(),
                        metaSet.isImmutable(), metaSet.isReference());
            }
            else
            {
                insert = sqlGenerator.insertInto(
                        EAV_M_SIMPLE_SET,
                        EAV_M_SIMPLE_SET.CONTAINING_ID,
                        EAV_M_SIMPLE_SET.CONTAINER_TYPE,
                        EAV_M_SIMPLE_SET.NAME,
                        EAV_M_SIMPLE_SET.TYPE_CODE,
                        EAV_M_SIMPLE_SET.IS_KEY,
                        EAV_M_SIMPLE_SET.IS_NULLABLE,
                        EAV_M_SIMPLE_SET.ARRAY_KEY_TYPE,
                        EAV_M_SIMPLE_SET.IS_IMMUTABLE,
                        EAV_M_SIMPLE_SET.IS_REFERENCE
                    ).values(parentId, parentType, attributeName, metaSet.getTypeCode().toString(),
                        metaAttribute.isKey(), metaAttribute.isNullable(),
                        metaSet.getArrayKeyType().toString(),
                        metaSet.isImmutable(), metaSet.isReference());
            }

            logger.debug(insert.toString());

            long t = 0;
            if(sqlStats != null)
            {
                t = System.nanoTime();
            }

            id = insertWithId(insert.getSQL(), insert.getBindValues().toArray());
            metaSet.setId(id);

            if(sqlStats != null)
            {
                sqlStats.put(insert.getSQL(), (System.nanoTime() - t) / 1000000);
            }
        }

        return id;
    }

    private long saveAttribute(IMetaType type, long parentId, int parentType, IMetaAttribute metaAttribute, String attributeName)
    {
        //String query;
        //Object[] args;
        InsertOnDuplicateStep insert;

        if(type.isSet())
        {
            throw new IllegalStateException(attributeName + " is an array, single value expected.");
        }

        if(type.isComplex())
        {
            long innerId = save((MetaClass)type);

            insert = sqlGenerator.insertInto(
                    EAV_M_COMPLEX_ATTRIBUTES,
                    EAV_M_COMPLEX_ATTRIBUTES.CONTAINING_ID,
                    EAV_M_COMPLEX_ATTRIBUTES.CONTAINER_TYPE,
                    EAV_M_COMPLEX_ATTRIBUTES.NAME,
                    EAV_M_COMPLEX_ATTRIBUTES.IS_KEY,
                    EAV_M_COMPLEX_ATTRIBUTES.IS_NULLABLE,
                    EAV_M_COMPLEX_ATTRIBUTES.CLASS_ID
                ).values(parentId, parentType, attributeName, metaAttribute.isKey(), metaAttribute.isNullable(),
                    innerId);
        }
        else
        {
            insert = sqlGenerator.insertInto(
                    EAV_M_SIMPLE_ATTRIBUTES,
                    EAV_M_SIMPLE_ATTRIBUTES.CONTAINING_ID,
                    EAV_M_SIMPLE_ATTRIBUTES.CONTAINER_TYPE,
                    EAV_M_SIMPLE_ATTRIBUTES.NAME,
                    EAV_M_SIMPLE_ATTRIBUTES.TYPE_CODE,
                    EAV_M_SIMPLE_ATTRIBUTES.IS_KEY,
                    EAV_M_SIMPLE_ATTRIBUTES.IS_NULLABLE
                ).values(parentId, parentType, attributeName, ((MetaValue)type).getTypeCode().toString(),
                    metaAttribute.isKey(), metaAttribute.isNullable());
        }

        logger.debug(insert.toString());

        long t = 0;
        if(sqlStats != null)
        {
            t = System.nanoTime();
        }

        long id = insertWithId(insert.getSQL(), insert.getBindValues().toArray());

        if(sqlStats != null)
        {
            sqlStats.put(insert.getSQL(), (System.nanoTime() - t) / 1000000);
        }

        return id;
    }

    private void insertAttributes(Set<String> addNames, MetaClass meta)
    {
        if(meta.getId() < 1)
        {
            throw new IllegalArgumentException("MetaClass must have an id filled before attributes insertion to DB");
        }

        for(String typeName : addNames)
        {
            IMetaAttribute metaAttribute = meta.getMetaAttribute(typeName);
            IMetaType metaType = metaAttribute.getMetaType();

            if(metaType.isSet())
            {
                saveSet(metaType, meta.getId(), ContainerTypes.CLASS, metaAttribute, typeName);
            }
            else
            {
                metaAttribute.setId(saveAttribute(metaType, meta.getId(), ContainerTypes.CLASS,
                        metaAttribute, typeName));
            }
        }
    }

    private void deleteAttributes(Set<String> deleteNames, MetaClass meta)
    {
        DeleteConditionStep delete;

        if(meta.getId() < 1)
        {
            throw new IllegalArgumentException("MetaClass must have an id filled before attributes deletion to DB");
        }

        for(String typeName : deleteNames)
        {
            if(meta.getMemberType(typeName).isComplex())
            {
                delete = sqlGenerator.delete(EAV_M_COMPLEX_ATTRIBUTES
                    ).where(EAV_M_COMPLEX_ATTRIBUTES.CONTAINING_ID.eq(meta.getId())
                    ).and(EAV_M_COMPLEX_ATTRIBUTES.CONTAINER_TYPE.eq(ContainerTypes.CLASS)
                    ).and(EAV_M_COMPLEX_ATTRIBUTES.NAME.eq(typeName));
            }
            else
            {
                delete = sqlGenerator.delete(EAV_M_SIMPLE_ATTRIBUTES
                    ).where(EAV_M_SIMPLE_ATTRIBUTES.CONTAINING_ID.eq(meta.getId())
                    ).and(EAV_M_SIMPLE_ATTRIBUTES.CONTAINER_TYPE.eq(ContainerTypes.CLASS)
                    ).and(EAV_M_SIMPLE_ATTRIBUTES.NAME.eq(typeName));
            }

            logger.debug(delete.toString());

            long t = 0;
            if(sqlStats != null)
            {
                t = System.nanoTime();
            }
            jdbcTemplate.update(delete.getSQL(), delete.getBindValues().toArray());
            if(sqlStats != null)
            {
                sqlStats.put(delete.getSQL(), (System.nanoTime() - t) / 1000000);
            }
        }
    }

    void loadSimpleAttributes(IMetaContainer meta)
    {
        SelectForUpdateStep select = sqlGenerator.select(
                EAV_M_SIMPLE_ATTRIBUTES.ID,
                EAV_M_SIMPLE_ATTRIBUTES.NAME,
                EAV_M_SIMPLE_ATTRIBUTES.TYPE_CODE,
                EAV_M_SIMPLE_ATTRIBUTES.CONTAINER_TYPE,
                EAV_M_SIMPLE_ATTRIBUTES.CONTAINING_ID,
                EAV_M_SIMPLE_ATTRIBUTES.IS_KEY,
                EAV_M_SIMPLE_ATTRIBUTES.IS_NULLABLE
            ).from(EAV_M_SIMPLE_ATTRIBUTES
        ).where(
                EAV_M_SIMPLE_ATTRIBUTES.CONTAINING_ID.eq(meta.getId())
        ).and(
                EAV_M_SIMPLE_ATTRIBUTES.CONTAINER_TYPE.eq(meta.getType())
        );

        logger.debug(select.toString());

        long t = 0;
        if(sqlStats != null)
        {
            t = System.nanoTime();
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(select.getSQL(), select.getBindValues().toArray());
        if(sqlStats != null)
        {
            sqlStats.put(select.getSQL(), (System.nanoTime() - t) / 1000000);
        }

        for (Map<String, Object> row : rows) {
            MetaAttribute metaAttirubute = new MetaAttribute(
                    ((Long)row.get("id")).longValue(),
                    (Boolean)row.get("is_key"),
                    (Boolean)row.get("is_nullable"));

            metaAttirubute.setMetaType(new MetaValue(DataTypes.valueOf((String) row.get("type_code"))));

            meta.setMetaAttribute((String) row.get("name"), metaAttirubute);
        }
    }

    void loadSimpleArrays(IMetaContainer meta)
    {
        SelectForUpdateStep select = sqlGenerator.select(
                EAV_M_SIMPLE_SET.ID,
                EAV_M_SIMPLE_SET.NAME,
                EAV_M_SIMPLE_SET.CONTAINER_TYPE,
                EAV_M_SIMPLE_SET.CONTAINING_ID,
                EAV_M_SIMPLE_SET.IS_KEY,
                EAV_M_SIMPLE_SET.IS_NULLABLE,
                EAV_M_SIMPLE_SET.TYPE_CODE,
                EAV_M_SIMPLE_SET.ARRAY_KEY_TYPE,
                EAV_M_SIMPLE_SET.IS_IMMUTABLE,
                EAV_M_SIMPLE_SET.IS_REFERENCE
            ).from(EAV_M_SIMPLE_SET
        ).where(EAV_M_SIMPLE_SET.CONTAINING_ID.eq(meta.getId())
        ).and(EAV_M_SIMPLE_SET.CONTAINER_TYPE.eq(meta.getType()));

        long t = 0;

        logger.debug(select.toString());

        if(sqlStats != null)
        {
            t = System.nanoTime();
        }
        List<Map<String, Object>>  rows = jdbcTemplate.queryForList(select.getSQL(), select.getBindValues().toArray());
        if(sqlStats != null)
        {
            sqlStats.put(select.getSQL(), (System.nanoTime() - t) / 1000000);
        }

        for (Map<String, Object> row : rows) {

            MetaAttribute metaAttribute = new MetaAttribute(
                    ((Long)row.get("id")).longValue(),
                    (Boolean)row.get("is_key"),
                    (Boolean)row.get("is_nullable"));


            MetaSet metaSet = new MetaSet(new MetaValue(DataTypes.valueOf((String) row.get("type_code"))));
            metaSet.setId(((Long)row.get("id")).longValue());

            metaSet.setArrayKeyType(ComplexKeyTypes.valueOf((String) row.get("array_key_type")));
            metaSet.setImmutable((Boolean)row.get("is_immutable"));
            metaSet.setReference((Boolean)row.get("is_reference"));

            metaAttribute.setMetaType(metaSet);

            meta.setMetaAttribute((String) row.get("name"), metaAttribute);


        }
    }

    void loadComplexAttributes(IMetaContainer meta)
    {
        SelectForUpdateStep select = sqlGenerator.select(
                EAV_M_COMPLEX_ATTRIBUTES.ID,
                EAV_M_COMPLEX_ATTRIBUTES.IS_KEY,
                EAV_M_COMPLEX_ATTRIBUTES.IS_NULLABLE,
                EAV_M_COMPLEX_ATTRIBUTES.NAME,
                EAV_M_COMPLEX_ATTRIBUTES.CONTAINER_TYPE,
                EAV_M_COMPLEX_ATTRIBUTES.CONTAINING_ID,
                EAV_M_COMPLEX_ATTRIBUTES.CLASS_ID,
                EAV_M_CLASSES.NAME.as("cname")
            ).from(EAV_M_COMPLEX_ATTRIBUTES).leftOuterJoin(EAV_M_CLASSES
        ).on(EAV_M_COMPLEX_ATTRIBUTES.CLASS_ID.eq(EAV_M_CLASSES.ID)
        ).where(EAV_M_COMPLEX_ATTRIBUTES.CONTAINING_ID.eq(meta.getId())
        ).and(EAV_M_COMPLEX_ATTRIBUTES.CONTAINER_TYPE.eq(meta.getType()));

        long t = 0;

        logger.debug(select.toString());

        if(sqlStats != null)
        {
            t = System.nanoTime();
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(select.getSQL(), select.getBindValues().toArray());
        if(sqlStats != null)
        {
            sqlStats.put(select.getSQL(), (System.nanoTime() - t) / 1000000);
        }

        for (Map<String, Object> row : rows) {
            MetaClass metaClass = load((Long)row.get("class_id"));

            MetaAttribute metaAttribute = new MetaAttribute(
                    ((Long)row.get("id")).longValue(),
                    (Boolean)row.get("is_key"),
                    (Boolean)row.get("is_nullable"));

            metaAttribute.setMetaType(metaClass);

            meta.setMetaAttribute((String) row.get("name"), metaAttribute);
        }
    }

    void loadComplexArrays(IMetaContainer meta)
    {
        SelectForUpdateStep select = sqlGenerator.select(
                EAV_M_COMPLEX_SET.ID,
                EAV_M_COMPLEX_SET.IS_NULLABLE,
                EAV_M_COMPLEX_SET.IS_KEY,
                EAV_M_COMPLEX_SET.NAME,
                EAV_M_COMPLEX_SET.CLASS_ID,
                EAV_M_COMPLEX_SET.ARRAY_KEY_TYPE,
                EAV_M_COMPLEX_SET.CONTAINER_TYPE,
                EAV_M_COMPLEX_SET.CONTAINING_ID,
                EAV_M_COMPLEX_SET.IS_IMMUTABLE,
                EAV_M_COMPLEX_SET.IS_REFERENCE,
                EAV_M_CLASSES.NAME.as("cname")
            ).from(EAV_M_COMPLEX_SET
        ).leftOuterJoin(EAV_M_CLASSES).on(EAV_M_COMPLEX_SET.CLASS_ID.eq(EAV_M_CLASSES.ID)
        ).where(EAV_M_COMPLEX_SET.CONTAINING_ID.eq(meta.getId())
        ).and(EAV_M_COMPLEX_SET.CONTAINER_TYPE.eq(meta.getType()));
        long t = 0;

        logger.debug(select.toString());

        if(sqlStats != null)
        {
            t = System.nanoTime();
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(select.getSQL(), select.getBindValues().toArray());
        if(sqlStats != null)
        {
            sqlStats.put(select.getSQL(), (System.nanoTime() - t) / 1000000);
        }

        for (Map<String, Object> row : rows) {
            MetaClass metaClass = load((Long)row.get("class_id"));

            MetaAttribute metaAttribute = new MetaAttribute(
                    ((Long)row.get("id")).longValue(),
                    (Boolean)row.get("is_key"),
                    (Boolean)row.get("is_nullable"));

            MetaSet metaSet = new MetaSet(metaClass);
            metaSet.setId(((Long)row.get("id")).longValue());
            metaSet.setArrayKeyType(ComplexKeyTypes.valueOf((String) row.get("array_key_type")));
            metaSet.setImmutable((Boolean)row.get("is_immutable"));
            metaSet.setReference((Boolean)row.get("is_reference"));

            metaAttribute.setMetaType(metaSet);

            meta.setMetaAttribute((String) row.get("name"), metaAttribute);
        }
    }

    void loadArrayArrays(IMetaContainer meta)
    {
        SelectForUpdateStep select = sqlGenerator.select(
                EAV_M_SET_OF_SETS.ID,
                EAV_M_SET_OF_SETS.NAME,
                EAV_M_SET_OF_SETS.IS_NULLABLE,
                EAV_M_SET_OF_SETS.IS_KEY,
                EAV_M_SET_OF_SETS.CONTAINER_TYPE,
                EAV_M_SET_OF_SETS.CONTAINING_ID,
                EAV_M_SET_OF_SETS.ARRAY_KEY_TYPE,
                EAV_M_SET_OF_SETS.IS_IMMUTABLE,
                EAV_M_SET_OF_SETS.IS_REFERENCE
            ).from(EAV_M_SET_OF_SETS
        ).where(EAV_M_SET_OF_SETS.CONTAINING_ID.eq(meta.getId())
        ).and(EAV_M_SET_OF_SETS.CONTAINER_TYPE.eq(meta.getType()));
        long t = 0;

        logger.debug(select.toString());

        if(sqlStats != null)
        {
            t = System.nanoTime();
        }
        List<Map<String, Object>>  rows = jdbcTemplate.queryForList(select.getSQL(), select.getBindValues().toArray());
        if(sqlStats != null)
        {
            sqlStats.put(select.getSQL(), (System.nanoTime() - t) / 1000000);
        }

        for (Map<String, Object> row : rows) {

            MetaAttribute metaAttribute = new MetaAttribute(
                    ((Long)row.get("id")).longValue(),
                    (Boolean)row.get("is_key"),
                    (Boolean)row.get("is_nullable"));



            MetaSet metaSet = new MetaSet();

            metaSet.setId(((Long)row.get("id")).longValue());
            metaSet.setArrayKeyType(ComplexKeyTypes.valueOf((String) row.get("array_key_type")));
            metaSet.setImmutable((Boolean)row.get("is_immutable"));
            metaSet.setReference((Boolean)row.get("is_reference"));

            loadSimpleArrays(metaSet);
            loadComplexArrays(metaSet);
            loadArrayArrays(metaSet);

            metaAttribute.setMetaType(metaSet);

            meta.setMetaAttribute((String) row.get("name"), metaAttribute);


        }
    }

    void loadAttributes(MetaClass meta) {
        if(meta.getId() < 1)
        {
            throw new IllegalStateException("Can't load atributes of metaclass without id!");
        }

        meta.removeMembers();

        loadSimpleAttributes(meta);
        loadSimpleArrays(meta);
        loadComplexAttributes(meta);
        loadComplexArrays(meta);
        loadArrayArrays(meta);
    }

	@Transactional
	public long save(MetaClass meta) {
        MetaClass dbMeta = new MetaClass(meta);

        try
        {
            loadClass(dbMeta, true);
            loadAttributes(dbMeta);
            meta.setId(dbMeta.getId());
            updateClass(meta);
        }
        catch(IllegalArgumentException e)
        {
            logger.debug("Class: " + meta.getClassName() + " not found.");
            createClass(dbMeta);
            dbMeta.removeMembers();
            meta.setId(dbMeta.getId());
            logger.debug("Class: " + meta.getClassName() + " created.");
        }

	    if(dbMeta.getId() < 1)
	    {
	    	throw new IllegalArgumentException("Can't determine meta id");
	    }

	    Set<String> oldNames = dbMeta.getMemberNames();
	    Set<String> newNames = meta.getMemberNames();

	    Set<String> updateNames = SetUtils.intersection(oldNames, newNames);
	    Set<String> deleteNames = SetUtils.difference(oldNames, newNames);
	    Set<String> addNames = SetUtils.difference(newNames, oldNames);

        for (String name : updateNames)
        {
            if(!meta.getMemberType(name).equals(dbMeta.getMemberType(name)))
            {
                deleteNames.add(name);
                addNames.add(name);
            }
        }

        deleteAttributes(deleteNames, dbMeta);
        insertAttributes(addNames, meta);

	    return dbMeta.getId();
	}

	@Override
	public MetaClass load(String className) {
		MetaClass meta = new MetaClass(className);

        loadClass(meta, false);
        loadAttributes(meta);

	    return meta;
	}

    @Override
    public MetaClass load(String className, Timestamp beginDate) {
        MetaClass meta = new MetaClass(className, beginDate);

        loadClass(meta, false);
        loadAttributes(meta);

        return meta;
    }

	@Override
	public MetaClass load(long id) {
        if(id < 1)
            return null;

        MetaClass meta = new MetaClass();
        meta.setId(id);

        loadClass(meta, false);
        loadAttributes(meta);

        return meta;
	}

    private void removeAllAttributes(long id, int type)
    {
        DeleteConditionStep delete = sqlGenerator.delete(EAV_M_SIMPLE_ATTRIBUTES
            ).where(EAV_M_SIMPLE_ATTRIBUTES.CONTAINING_ID.eq(id)
        ).and(EAV_M_SIMPLE_ATTRIBUTES.CONTAINER_TYPE.eq(type));

        logger.debug(delete.toString());

        long t = 0;
        if(sqlStats != null)
        {
            t = System.nanoTime();
        }
        jdbcTemplate.update(delete.getSQL(), delete.getBindValues().toArray());
        if(sqlStats != null)
        {
            sqlStats.put(delete.getSQL(), (System.nanoTime() - t) / 1000000);
        }

        delete = sqlGenerator.delete(EAV_M_COMPLEX_ATTRIBUTES
            ).where(EAV_M_COMPLEX_ATTRIBUTES.CONTAINING_ID.eq(id)
            ).and(EAV_M_COMPLEX_ATTRIBUTES.CONTAINER_TYPE.eq(type));

        logger.debug(delete.toString());

        t = 0;
        if(sqlStats != null)
        {
            t = System.nanoTime();
        }
        jdbcTemplate.update(delete.getSQL(), delete.getBindValues().toArray());
        if(sqlStats != null)
        {
            sqlStats.put(delete.getSQL(), (System.nanoTime() - t) / 1000000);
        }
    }

    private void removeSet(MetaSet set)
    {
        if (set.getMemberType().isSet())
        {
            removeSet((MetaSet)set.getMemberType());
            removeAllAttributes(set.getId(), ContainerTypes.SET);
        }
        else
        {
            if (set.getMemberType().isComplex())
            {
                remove((MetaClass)set.getMemberType());
            }
        }
    }

	@Override
    @Transactional
	public void remove(MetaClass metaClass) {
		if(metaClass.getId() < 1)
        {
            throw new IllegalArgumentException("Can't remove MetaClass without id");
        }

        long t = 0;

        for (String arrayName : metaClass.getArrayArrayAttributesNames())
        {
            removeSet(((MetaSet) metaClass.getMemberType(arrayName)));
        }

        //delete all class attributes
        removeAllAttributes(metaClass.getId(), ContainerTypes.CLASS);

        //delete class
        DeleteConditionStep delete = sqlGenerator.delete(EAV_M_CLASSES).where(EAV_M_CLASSES.ID.eq(metaClass.getId()));

        logger.debug(delete.toString());

        if(sqlStats != null)
        {
            t = System.nanoTime();
        }
        jdbcTemplate.update(delete.getSQL(), delete.getBindValues().toArray());
        if(sqlStats != null)
        {
            sqlStats.put(delete.getSQL(), (System.nanoTime() - t) / 1000000);
        }
	}

    @SuppressWarnings("UnusedDeclaration")
    public Executor getSqlGenerator()
    {
        return sqlGenerator;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setSqlGenerator(Executor sqlGenerator)
    {
        this.sqlGenerator = sqlGenerator;
    }
}

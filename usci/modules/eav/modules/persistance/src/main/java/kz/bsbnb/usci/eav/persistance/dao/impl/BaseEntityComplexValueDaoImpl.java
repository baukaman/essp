package kz.bsbnb.usci.eav.persistance.dao.impl;

import kz.bsbnb.usci.eav.util.Errors;
import kz.bsbnb.usci.eav.model.base.IBaseContainer;
import kz.bsbnb.usci.eav.model.base.IBaseEntity;
import kz.bsbnb.usci.eav.model.base.IBaseValue;
import kz.bsbnb.usci.eav.model.base.impl.BaseValueFactory;
import kz.bsbnb.usci.eav.model.meta.IMetaAttribute;
import kz.bsbnb.usci.eav.model.meta.IMetaClass;
import kz.bsbnb.usci.eav.model.meta.IMetaType;
import kz.bsbnb.usci.eav.model.meta.impl.MetaContainerTypes;
import kz.bsbnb.usci.eav.model.persistable.IPersistable;
import kz.bsbnb.usci.eav.persistance.dao.IBaseEntityComplexValueDao;
import kz.bsbnb.usci.eav.persistance.dao.IBaseEntityDao;
import kz.bsbnb.usci.eav.persistance.dao.IBaseEntityLoadDao;
import kz.bsbnb.usci.eav.persistance.dao.IBaseEntityProcessorDao;
import kz.bsbnb.usci.eav.persistance.db.JDBCSupport;
import kz.bsbnb.usci.eav.repository.IBatchRepository;
import kz.bsbnb.usci.eav.util.DataUtils;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;

import static kz.bsbnb.eav.persistance.generated.Tables.*;

@Repository
public class BaseEntityComplexValueDaoImpl extends JDBCSupport implements IBaseEntityComplexValueDao {
    private final Logger logger = LoggerFactory.getLogger(BaseEntityComplexValueDaoImpl.class);

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    private DSLContext context;

    @Autowired
    IBatchRepository batchRepository;

    @Autowired
    IBaseEntityDao baseEntityDao;

    @Autowired
    IBaseEntityProcessorDao baseEntityProcessorDao;

    @Autowired
    IBaseEntityLoadDao baseEntityLoadDao;

    @Override
    public long insert(IPersistable persistable) {
        IBaseValue baseValue = (IBaseValue) persistable;
        IBaseEntity baseEntity = (IBaseEntity) baseValue.getValue();

        Insert insert = context
                .insertInto(EAV_BE_COMPLEX_VALUES)
                .set(EAV_BE_COMPLEX_VALUES.ENTITY_ID, baseValue.getBaseContainer().getId())
                .set(EAV_BE_COMPLEX_VALUES.ATTRIBUTE_ID, baseValue.getMetaAttribute().getId())
                .set(EAV_BE_COMPLEX_VALUES.CREDITOR_ID, baseValue.getCreditorId())
                .set(EAV_BE_COMPLEX_VALUES.REPORT_DATE, DataUtils.convert(baseValue.getRepDate()))
                .set(EAV_BE_COMPLEX_VALUES.ENTITY_VALUE_ID, baseEntity.getId())
                .set(EAV_BE_COMPLEX_VALUES.IS_CLOSED, DataUtils.convert(baseValue.isClosed()))
                .set(EAV_BE_COMPLEX_VALUES.IS_LAST, DataUtils.convert(baseValue.isLast()));

        long baseValueId = insertWithId(insert.getSQL(), insert.getBindValues().toArray());

        baseValue.setId(baseValueId);

        return baseValueId;
    }

    @Override
    public void update(IPersistable persistable) {
        IBaseValue baseValue = (IBaseValue) persistable;
        IBaseEntity baseEntity = (IBaseEntity) baseValue.getValue();

        String tableAlias = "cv";
        Update update = context
                .update(EAV_BE_COMPLEX_VALUES.as(tableAlias))
                .set(EAV_BE_COMPLEX_VALUES.as(tableAlias).ENTITY_ID, baseValue.getBaseContainer().getId())
                .set(EAV_BE_COMPLEX_VALUES.as(tableAlias).ATTRIBUTE_ID, baseValue.getMetaAttribute().getId())
                .set(EAV_BE_COMPLEX_VALUES.as(tableAlias).CREDITOR_ID, baseValue.getCreditorId())
                .set(EAV_BE_COMPLEX_VALUES.as(tableAlias).REPORT_DATE, DataUtils.convert(baseValue.getRepDate()))
                .set(EAV_BE_COMPLEX_VALUES.as(tableAlias).ENTITY_VALUE_ID, baseEntity.getId())
                .set(EAV_BE_COMPLEX_VALUES.as(tableAlias).IS_CLOSED, DataUtils.convert(baseValue.isClosed()))
                .set(EAV_BE_COMPLEX_VALUES.as(tableAlias).IS_LAST, DataUtils.convert(baseValue.isLast()))
                .where(EAV_BE_COMPLEX_VALUES.as(tableAlias).ID.equal(baseValue.getId()));

        int count = updateWithStats(update.getSQL(), update.getBindValues().toArray());

        if (count != 1)
            throw new IllegalStateException(Errors.getMessage(Errors.E88, count, baseValue.getId()));
    }

    @Override
    public void delete(IPersistable persistable) {
        String tableAlias = "cv";
        Delete delete = context
                .delete(EAV_BE_COMPLEX_VALUES.as(tableAlias))
                .where(EAV_BE_COMPLEX_VALUES.as(tableAlias).ID.equal(persistable.getId()));

        logger.debug(delete.toString());

        int count = updateWithStats(delete.getSQL(), delete.getBindValues().toArray());

        if (count != 1)
            throw new IllegalStateException(Errors.getMessage(Errors.E87, count, persistable.getId()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public IBaseValue getNextBaseValue(IBaseValue baseValue) {
        IMetaAttribute metaAttribute = baseValue.getMetaAttribute();
        if (metaAttribute == null)
            throw new IllegalStateException(Errors.getMessage(Errors.E80));

        if (metaAttribute.getId() < 1)
            throw new IllegalStateException(Errors.getMessage(Errors.E81));

        IBaseContainer baseContainer = baseValue.getBaseContainer();
        if (baseContainer == null)
            throw new IllegalStateException(Errors.getMessage(Errors.E82, baseValue.getMetaAttribute().getName()));

        if (baseContainer.getId() < 1)
            return null;

        IBaseEntity baseEntity = (IBaseEntity) baseContainer;
        IMetaClass metaClass = baseEntity.getMeta();

        IBaseEntity parentBaseEntity = (IBaseEntity) baseContainer;
        IMetaType metaType = metaAttribute.getMetaType();
        IBaseValue nextBaseValue = null;

        String tableAlias = "bv";
        String subqueryAlias = "bvn";

        Table subqueryTable = context
                .select(DSL.rank().over()
                                .orderBy(EAV_BE_COMPLEX_VALUES.as(tableAlias).REPORT_DATE.asc()).as("num_pp"),
                        EAV_BE_COMPLEX_VALUES.as(tableAlias).ID,
                        EAV_BE_COMPLEX_VALUES.as(tableAlias).REPORT_DATE,
                        EAV_BE_COMPLEX_VALUES.as(tableAlias).CREDITOR_ID,
                        EAV_BE_COMPLEX_VALUES.as(tableAlias).ENTITY_VALUE_ID,
                        EAV_BE_COMPLEX_VALUES.as(tableAlias).IS_CLOSED,
                        EAV_BE_COMPLEX_VALUES.as(tableAlias).IS_LAST)
                .from(EAV_BE_COMPLEX_VALUES.as(tableAlias))
                .where(EAV_BE_COMPLEX_VALUES.as(tableAlias).ENTITY_ID.equal(parentBaseEntity.getId()))
                .and(EAV_BE_COMPLEX_VALUES.as(tableAlias).ATTRIBUTE_ID.equal(metaAttribute.getId()))
                .and(EAV_BE_COMPLEX_VALUES.as(tableAlias).REPORT_DATE.greaterThan(
                        DataUtils.convert(baseValue.getRepDate())))
                .and(EAV_BE_COMPLEX_VALUES.as(tableAlias).CREDITOR_ID.equal(baseValue.getCreditorId()))
                .asTable(subqueryAlias);

        Select select = context
                .select(subqueryTable.field(EAV_BE_COMPLEX_VALUES.ID),
                        subqueryTable.field(EAV_BE_COMPLEX_VALUES.REPORT_DATE),
                        subqueryTable.field(EAV_BE_COMPLEX_VALUES.CREDITOR_ID),
                        subqueryTable.field(EAV_BE_COMPLEX_VALUES.ENTITY_VALUE_ID),
                        subqueryTable.field(EAV_BE_COMPLEX_VALUES.IS_CLOSED),
                        subqueryTable.field(EAV_BE_COMPLEX_VALUES.IS_LAST))
                .from(subqueryTable)
                .where(subqueryTable.field("num_pp").cast(Integer.class).equal(1));


        logger.debug(select.toString());
        List<Map<String, Object>> rows = queryForListWithStats(select.getSQL(), select.getBindValues().toArray());

        if (rows.size() > 1)
            throw new IllegalStateException(Errors.getMessage(Errors.E83, metaAttribute.getName()));

        if (rows.size() == 1) {
            Map<String, Object> row = rows.iterator().next();

            long id = ((BigDecimal) row
                    .get(EAV_BE_COMPLEX_VALUES.ID.getName())).longValue();

            long creditorId = ((BigDecimal) row
                    .get(EAV_BE_COMPLEX_VALUES.CREDITOR_ID.getName())).longValue();

            boolean closed = ((BigDecimal) row
                    .get(EAV_BE_COMPLEX_VALUES.IS_CLOSED.getName())).longValue() == 1;

            boolean last = ((BigDecimal) row
                    .get(EAV_BE_COMPLEX_VALUES.IS_LAST.getName())).longValue() == 1;

            long entityValueId = ((BigDecimal) row
                    .get(EAV_BE_COMPLEX_VALUES.ENTITY_VALUE_ID.getName())).longValue();

            Date reportDate = DataUtils.convertToSQLDate((Timestamp) row
                    .get(EAV_BE_COMPLEX_VALUES.REPORT_DATE.getName()));

            IBaseEntity childBaseEntity = baseEntityLoadDao.loadByMaxReportDate(entityValueId, reportDate);

            nextBaseValue = BaseValueFactory.create(
                    metaClass.getType(),
                    metaType,
                    id,
                    creditorId,
                    reportDate,
                    childBaseEntity,
                    closed,
                    last);
        }

        return nextBaseValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public IBaseValue getPreviousBaseValue(IBaseValue baseValue) {
        IMetaAttribute metaAttribute = baseValue.getMetaAttribute();
        if (metaAttribute == null)
            throw new IllegalStateException(Errors.getMessage(Errors.E80));

        if (metaAttribute.getId() < 1)
            throw new IllegalStateException(Errors.getMessage(Errors.E81));

        IBaseContainer baseContainer = baseValue.getBaseContainer();
        if (baseContainer == null)
            throw new IllegalStateException(Errors.getMessage(Errors.E82, baseValue.getMetaAttribute().getName()));

        if (baseContainer.getId() < 1)
            return null;

        IBaseEntity baseEntity = (IBaseEntity) baseContainer;
        IMetaClass metaClass = baseEntity.getMeta();

        IBaseEntity parentBaseEntity = (IBaseEntity) baseContainer;
        IMetaType metaType = metaAttribute.getMetaType();
        IBaseValue previousBaseValue = null;

        String tableAlias = "bv";
        String subqueryAlias = "bvn";

        Table subqueryTable = context
                .select(DSL.rank().over()
                                .orderBy(EAV_BE_COMPLEX_VALUES.as(tableAlias).REPORT_DATE.desc()).as("num_pp"),
                        EAV_BE_COMPLEX_VALUES.as(tableAlias).ID,
                        EAV_BE_COMPLEX_VALUES.as(tableAlias).REPORT_DATE,
                        EAV_BE_COMPLEX_VALUES.as(tableAlias).CREDITOR_ID,
                        EAV_BE_COMPLEX_VALUES.as(tableAlias).ENTITY_VALUE_ID,
                        EAV_BE_COMPLEX_VALUES.as(tableAlias).IS_CLOSED,
                        EAV_BE_COMPLEX_VALUES.as(tableAlias).IS_LAST)
                .from(EAV_BE_COMPLEX_VALUES.as(tableAlias))
                .where(EAV_BE_COMPLEX_VALUES.as(tableAlias).ENTITY_ID.equal(parentBaseEntity.getId()))
                .and(EAV_BE_COMPLEX_VALUES.as(tableAlias).ATTRIBUTE_ID.equal(metaAttribute.getId()))
                .and(EAV_BE_COMPLEX_VALUES.as(tableAlias).REPORT_DATE.lessThan(
                        DataUtils.convert(baseValue.getRepDate())))
                .and(EAV_BE_COMPLEX_VALUES.as(tableAlias).CREDITOR_ID.equal(baseValue.getCreditorId()))
                .asTable(subqueryAlias);

        Select select = context
                .select(subqueryTable.field(EAV_BE_COMPLEX_VALUES.ID),
                        subqueryTable.field(EAV_BE_COMPLEX_VALUES.REPORT_DATE),
                        subqueryTable.field(EAV_BE_COMPLEX_VALUES.CREDITOR_ID),
                        subqueryTable.field(EAV_BE_COMPLEX_VALUES.ENTITY_VALUE_ID),
                        subqueryTable.field(EAV_BE_COMPLEX_VALUES.IS_CLOSED),
                        subqueryTable.field(EAV_BE_COMPLEX_VALUES.IS_LAST))
                .from(subqueryTable)
                .where(subqueryTable.field("num_pp").cast(Integer.class).equal(1));


        logger.debug(select.toString());
        List<Map<String, Object>> rows = queryForListWithStats(select.getSQL(), select.getBindValues().toArray());

        if (rows.size() > 1)
            throw new IllegalStateException(Errors.getMessage(Errors.E83, metaAttribute.getName()));

        if (rows.size() == 1) {
            Map<String, Object> row = rows.iterator().next();

            long id = ((BigDecimal) row
                    .get(EAV_BE_COMPLEX_VALUES.ID.getName())).longValue();

            long creditorId = ((BigDecimal) row
                    .get(EAV_BE_COMPLEX_VALUES.CREDITOR_ID.getName())).longValue();


            boolean closed = ((BigDecimal) row
                    .get(EAV_BE_COMPLEX_VALUES.IS_CLOSED.getName())).longValue() == 1;

            boolean last = ((BigDecimal) row
                    .get(EAV_BE_COMPLEX_VALUES.IS_LAST.getName())).longValue() == 1;

            long entityValueId = ((BigDecimal) row
                    .get(EAV_BE_COMPLEX_VALUES.ENTITY_VALUE_ID.getName())).longValue();

            Date reportDate = DataUtils.convertToSQLDate((Timestamp) row
                    .get(EAV_BE_COMPLEX_VALUES.REPORT_DATE.getName()));

            IBaseEntity childBaseEntity = baseEntityLoadDao.loadByMaxReportDate(entityValueId, reportDate);

            previousBaseValue = BaseValueFactory.create(
                    metaClass.getType(),
                    metaType,
                    id,
                    creditorId,
                    reportDate,
                    childBaseEntity,
                    closed,
                    last);
        }

        return previousBaseValue;
    }

    @Override
    public IBaseValue getClosedBaseValue(IBaseValue baseValue) {
        IMetaAttribute metaAttribute = baseValue.getMetaAttribute();
        if (metaAttribute == null)
            throw new IllegalStateException(Errors.getMessage(Errors.E80));

        if (metaAttribute.getId() < 1)
            throw new IllegalStateException(Errors.getMessage(Errors.E81));

        IBaseContainer baseContainer = baseValue.getBaseContainer();
        if (baseContainer == null)
            throw new IllegalStateException(Errors.getMessage(Errors.E82, baseValue.getMetaAttribute().getName()));

        if (baseContainer.getId() < 1)
            return null;

        IMetaType metaType = metaAttribute.getMetaType();
        IBaseValue closedBaseValue = null;

        String tableAlias = "cv";
        Select select = context
                .select(EAV_BE_COMPLEX_VALUES.as(tableAlias).ID,
                        EAV_BE_COMPLEX_VALUES.as(tableAlias).ENTITY_VALUE_ID,
                        EAV_BE_COMPLEX_VALUES.as(tableAlias).CREDITOR_ID,
                        EAV_BE_COMPLEX_VALUES.as(tableAlias).IS_LAST)
                .from(EAV_BE_COMPLEX_VALUES.as(tableAlias))
                .where(EAV_BE_COMPLEX_VALUES.as(tableAlias).ENTITY_ID.equal(baseContainer.getId()))
                .and(EAV_BE_COMPLEX_VALUES.as(tableAlias).ATTRIBUTE_ID.equal(metaAttribute.getId()))
                .and(EAV_BE_COMPLEX_VALUES.as(tableAlias).REPORT_DATE.lessOrEqual(
                        DataUtils.convert(baseValue.getRepDate())))
                .and(EAV_BE_COMPLEX_VALUES.as(tableAlias).CREDITOR_ID.equal(baseValue.getCreditorId()))
                .and(EAV_BE_COMPLEX_VALUES.as(tableAlias).IS_CLOSED.equal(DataUtils.convert(true)));

        logger.debug(select.toString());
        List<Map<String, Object>> rows = queryForListWithStats(select.getSQL(), select.getBindValues().toArray());

        if (rows.size() > 1)
            throw new IllegalStateException(Errors.getMessage(Errors.E83, metaAttribute.getName()));

        if (rows.size() == 1) {
            Map<String, Object> row = rows.iterator().next();

            long id = ((BigDecimal) row
                    .get(EAV_BE_COMPLEX_VALUES.ID.getName())).longValue();

            long creditorId = ((BigDecimal) row
                    .get(EAV_BE_COMPLEX_VALUES.CREDITOR_ID.getName())).longValue();

            boolean last = ((BigDecimal) row
                    .get(EAV_BE_COMPLEX_VALUES.IS_LAST.getName())).longValue() == 1;

            long entityValueId = ((BigDecimal) row
                    .get(EAV_BE_COMPLEX_VALUES.ENTITY_VALUE_ID.getName())).longValue();

            IBaseEntity childBaseEntity = baseEntityLoadDao
                    .loadByMaxReportDate(entityValueId, baseValue.getRepDate());

            closedBaseValue = BaseValueFactory.create(
                    MetaContainerTypes.META_CLASS,
                    metaType,
                    id,
                    creditorId,
                    baseValue.getRepDate(),
                    childBaseEntity,
                    true,
                    last);
        }

        return closedBaseValue;
    }

    @Override
    public IBaseValue getLastBaseValue(IBaseValue baseValue) {
        IMetaAttribute metaAttribute = baseValue.getMetaAttribute();
        if (metaAttribute == null)
            throw new IllegalStateException(Errors.getMessage(Errors.E80));

        if (metaAttribute.getId() < 1)
            throw new IllegalStateException(Errors.getMessage(Errors.E81));

        IBaseContainer baseContainer = baseValue.getBaseContainer();
        if (baseContainer == null)
            throw new IllegalStateException(Errors.getMessage(Errors.E82, baseValue.getMetaAttribute().getName()));

        if (baseContainer.getId() < 1)
            return null;

        IMetaType metaType = metaAttribute.getMetaType();
        IBaseValue lastBaseValue = null;

        String tableAlias = "bv";
        Select select = context
                .select(EAV_BE_COMPLEX_VALUES.as(tableAlias).ID,
                        EAV_BE_COMPLEX_VALUES.as(tableAlias).REPORT_DATE,
                        EAV_BE_COMPLEX_VALUES.as(tableAlias).CREDITOR_ID,
                        EAV_BE_COMPLEX_VALUES.as(tableAlias).ENTITY_VALUE_ID,
                        EAV_BE_COMPLEX_VALUES.as(tableAlias).IS_LAST,
                        EAV_BE_COMPLEX_VALUES.as(tableAlias).IS_CLOSED)
                .from(EAV_BE_COMPLEX_VALUES.as(tableAlias))
                .where(EAV_BE_COMPLEX_VALUES.as(tableAlias).ENTITY_ID.equal(baseContainer.getId()))
                .and(EAV_BE_COMPLEX_VALUES.as(tableAlias).ATTRIBUTE_ID.equal(metaAttribute.getId()))
                .and(EAV_BE_COMPLEX_VALUES.as(tableAlias).CREDITOR_ID.equal(baseValue.getCreditorId()))
                .and(EAV_BE_COMPLEX_VALUES.as(tableAlias).IS_LAST.equal(DataUtils.convert(true)));

        logger.debug(select.toString());
        List<Map<String, Object>> rows = queryForListWithStats(select.getSQL(), select.getBindValues().toArray());

        if (rows.size() > 1)
            throw new IllegalStateException(Errors.getMessage(Errors.E83, metaAttribute.getName()));

        if (rows.size() == 1) {
            Map<String, Object> row = rows.iterator().next();

            long id = ((BigDecimal) row
                    .get(EAV_BE_COMPLEX_VALUES.ID.getName())).longValue();

            long creditorId = ((BigDecimal) row
                    .get(EAV_BE_COMPLEX_VALUES.CREDITOR_ID.getName())).longValue();

            boolean closed = ((BigDecimal) row
                    .get(EAV_BE_COMPLEX_VALUES.IS_CLOSED.getName())).longValue() == 1;

            long entityValueId = ((BigDecimal) row
                    .get(EAV_BE_COMPLEX_VALUES.ENTITY_VALUE_ID.getName())).longValue();

            Date reportDate = DataUtils.convertToSQLDate((Timestamp) row
                    .get(EAV_BE_COMPLEX_VALUES.REPORT_DATE.getName()));

            IBaseEntity childBaseEntity = baseEntityLoadDao.loadByMaxReportDate(entityValueId, reportDate);

            lastBaseValue = BaseValueFactory.create(
                    MetaContainerTypes.META_CLASS,
                    metaType,
                    id,
                    creditorId,
                    reportDate,
                    childBaseEntity,
                    closed,
                    true);
        }

        return lastBaseValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void loadBaseValues(IBaseEntity baseEntity, Date actualReportDate) {
        IMetaClass metaClass = baseEntity.getMeta();

        Table tableOfAttributes = EAV_M_COMPLEX_ATTRIBUTES.as("a");
        Table tableOfValues = EAV_BE_COMPLEX_VALUES.as("v");
        Select select;

        Table tableNumbering = context
                .select(DSL.rank().over()
                                .partitionBy(tableOfValues.field(EAV_BE_COMPLEX_VALUES.ATTRIBUTE_ID))
                                .orderBy(tableOfValues.field(EAV_BE_COMPLEX_VALUES.REPORT_DATE).desc())
                                .as("num_pp"),
                        tableOfValues.field(EAV_BE_COMPLEX_VALUES.ID),
                        tableOfValues.field(EAV_BE_COMPLEX_VALUES.ENTITY_ID),
                        tableOfValues.field(EAV_BE_COMPLEX_VALUES.ATTRIBUTE_ID),
                        tableOfValues.field(EAV_BE_COMPLEX_VALUES.CREDITOR_ID),
                        tableOfValues.field(EAV_BE_COMPLEX_VALUES.ENTITY_VALUE_ID),
                        tableOfValues.field(EAV_BE_COMPLEX_VALUES.REPORT_DATE),
                        tableOfValues.field(EAV_BE_COMPLEX_VALUES.IS_CLOSED),
                        tableOfValues.field(EAV_BE_COMPLEX_VALUES.IS_LAST))
                .from(tableOfValues)
                .where(tableOfValues.field(EAV_BE_COMPLEX_VALUES.ENTITY_ID).eq(baseEntity.getId()))
                .and(tableOfValues.field(EAV_BE_COMPLEX_VALUES.REPORT_DATE)
                        .lessOrEqual(DataUtils.convert(actualReportDate)))
                .asTable("vn");

        select = context
                .select(tableOfAttributes.field(EAV_M_COMPLEX_ATTRIBUTES.NAME),
                        tableNumbering.field(EAV_BE_COMPLEX_VALUES.ID),
                        tableNumbering.field(EAV_BE_COMPLEX_VALUES.REPORT_DATE),
                        tableNumbering.field(EAV_BE_COMPLEX_VALUES.CREDITOR_ID),
                        tableNumbering.field(EAV_BE_COMPLEX_VALUES.ENTITY_VALUE_ID),
                        tableNumbering.field(EAV_BE_COMPLEX_VALUES.IS_CLOSED),
                        tableNumbering.field(EAV_BE_COMPLEX_VALUES.IS_LAST))
                .from(tableNumbering)
                .join(tableOfAttributes)
                .on(tableNumbering.field(EAV_BE_COMPLEX_VALUES.ATTRIBUTE_ID)
                        .eq(tableOfAttributes.field(EAV_M_COMPLEX_ATTRIBUTES.ID)))
                .where(tableNumbering.field("num_pp").cast(Integer.class).equal(1))
                .and((tableNumbering.field(EAV_BE_COMPLEX_VALUES.IS_CLOSED).equal(false)
                        .and(tableOfAttributes.field(EAV_M_COMPLEX_ATTRIBUTES.IS_FINAL).equal(false)))
                        .or(tableNumbering.field(EAV_BE_COMPLEX_VALUES.REPORT_DATE).equal(actualReportDate)
                                .and(tableOfAttributes.field(EAV_M_COMPLEX_ATTRIBUTES.IS_FINAL).equal(true))));

        logger.debug(select.toString());

        List<Map<String, Object>> rows = queryForListWithStats(select.getSQL(), select.getBindValues().toArray());

        for (Map<String, Object> row : rows) {
            long id = ((BigDecimal) row.get(EAV_BE_COMPLEX_VALUES.ID.getName())).longValue();

            long creditorId = ((BigDecimal) row.get(EAV_BE_COMPLEX_VALUES.CREDITOR_ID.getName())).longValue();

            long entityValueId = ((BigDecimal) row.get(EAV_BE_COMPLEX_VALUES.ENTITY_VALUE_ID.getName())).longValue();

            boolean isClosed = ((BigDecimal) row.get(EAV_BE_COMPLEX_VALUES.IS_CLOSED.getName())).longValue() == 1;

            boolean isLast = ((BigDecimal) row.get(EAV_BE_COMPLEX_VALUES.IS_LAST.getName())).longValue() == 1;

            Date reportDate = DataUtils.convertToSQLDate((Timestamp)
                    row.get(EAV_BE_COMPLEX_VALUES.REPORT_DATE.getName()));

            String attribute = (String) row.get(EAV_M_COMPLEX_ATTRIBUTES.NAME.getName());

            IMetaType metaType = metaClass.getMemberType(attribute);

            IBaseEntity childBaseEntity = baseEntityLoadDao.loadByMaxReportDate(entityValueId, actualReportDate);

            baseEntity.put(attribute, BaseValueFactory.create(
                    MetaContainerTypes.META_CLASS,
                    metaType,
                    id,
                    creditorId,
                    reportDate,
                    childBaseEntity,
                    isClosed,
                    isLast));
        }
    }

    @Override
    public void deleteAll(long baseEntityId) {
        Set<Long> childBaseEntityIds = getChildBaseEntityIds(baseEntityId);

        String tableAlias = "cv";
        Delete delete = context
                .delete(EAV_BE_COMPLEX_VALUES.as(tableAlias))
                .where(EAV_BE_COMPLEX_VALUES.as(tableAlias).ENTITY_ID.equal(baseEntityId));

        logger.debug(delete.toString());
        updateWithStats(delete.getSQL(), delete.getBindValues().toArray());

        for (long childBaseEntityId : childBaseEntityIds)
            baseEntityDao.deleteRecursive(childBaseEntityId);
    }

    @Override
    public Set<Long> getChildBaseEntityIds(long parentBaseEntityId) {
        Set<Long> baseEntityIds = new HashSet<>();

        String tableAlias = "bv";
        Select select = context
                .select(EAV_BE_COMPLEX_VALUES.as(tableAlias).ENTITY_VALUE_ID)
                .from(EAV_BE_COMPLEX_VALUES.as(tableAlias))
                .where(EAV_BE_COMPLEX_VALUES.as(tableAlias).ENTITY_ID.equal(parentBaseEntityId))
                .groupBy(EAV_BE_COMPLEX_VALUES.as(tableAlias).ENTITY_VALUE_ID);

        logger.debug(select.toString());
        List<Map<String, Object>> rows = queryForListWithStats(select.getSQL(), select.getBindValues().toArray());

        if (rows.size() > 0) {
            for (Map<String, Object> row : rows) {
                long childBaseEntityId = ((BigDecimal) row
                        .get(EAV_BE_COMPLEX_VALUES.ENTITY_VALUE_ID.getName())).longValue();
                baseEntityIds.add(childBaseEntityId);
            }
        }

        return baseEntityIds;
    }

    @Override
    public Set<Long> getChildBaseEntityIdsWithoutRefs(long parentBaseEntityId) {
        Set<Long> baseEntityIds = new HashSet<Long>();

        String entitiesTableAlias = "e";
        String classesTableAlias = "c";
        String complexValuesTableAlias = "cv";
        Select select = context
                .select(EAV_BE_ENTITIES.as(entitiesTableAlias).ID)
                .from(EAV_BE_ENTITIES.as(entitiesTableAlias))
                .join(EAV_M_CLASSES.as(classesTableAlias))
                .on(EAV_BE_ENTITIES.as(entitiesTableAlias).CLASS_ID.equal(EAV_M_CLASSES.as(classesTableAlias).ID))
                .join(EAV_BE_COMPLEX_VALUES.as(complexValuesTableAlias))
                .on(EAV_BE_COMPLEX_VALUES.as(complexValuesTableAlias).ENTITY_VALUE_ID
                        .equal(EAV_BE_ENTITIES.as(entitiesTableAlias).ID))
                .where(EAV_BE_COMPLEX_VALUES.as(complexValuesTableAlias).ENTITY_ID.equal(parentBaseEntityId))
                .and(EAV_M_CLASSES.as(classesTableAlias).IS_REFERENCE.equal(DataUtils.convert(false)))
                .groupBy(EAV_BE_ENTITIES.as(entitiesTableAlias).ID);

        logger.debug(select.toString());
        List<Map<String, Object>> rows = queryForListWithStats(select.getSQL(), select.getBindValues().toArray());

        if (rows.size() > 0) {
            for (Map<String, Object> row : rows) {
                long childBaseEntityId = ((BigDecimal) row
                        .get(EAV_BE_ENTITIES.ID.getName())).longValue();
                baseEntityIds.add(childBaseEntityId);
            }
        }

        return baseEntityIds;
    }

    public boolean isSingleBaseValue(IBaseValue baseValue) {
        IMetaAttribute metaAttribute = baseValue.getMetaAttribute();
        IBaseEntity childBaseEntity = (IBaseEntity) baseValue.getValue();

        String entitiesTableAlias = "e";
        String complexValuesTableAlias = "cv";
        Select select = context
                .select(EAV_BE_ENTITIES.as(entitiesTableAlias).ID)
                .from(EAV_BE_ENTITIES.as(entitiesTableAlias))
                .where(EAV_BE_ENTITIES.as(entitiesTableAlias).ID.equal(childBaseEntity.getId()))
                .and(DSL.exists(context
                        .select(EAV_BE_COMPLEX_VALUES.as(complexValuesTableAlias).ID)
                        .from(EAV_BE_COMPLEX_VALUES.as(complexValuesTableAlias))
                        .where(EAV_BE_COMPLEX_VALUES.as(complexValuesTableAlias).ATTRIBUTE_ID.
                                equal(metaAttribute.getId()))
                        .and(EAV_BE_COMPLEX_VALUES.as(complexValuesTableAlias).ENTITY_VALUE_ID
                                .equal(EAV_BE_ENTITIES.as(entitiesTableAlias).ID))
                        .and(EAV_BE_COMPLEX_VALUES.as(complexValuesTableAlias).ID.
                                notEqual(baseValue.getId()))));

        logger.debug(select.toString());
        List<Map<String, Object>> rows = queryForListWithStats(select.getSQL(), select.getBindValues().toArray());

        return rows.size() == 0;
    }

}

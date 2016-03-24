package kz.bsbnb.usci.eav.persistance.dao.impl;

import kz.bsbnb.usci.eav.util.Errors;
import kz.bsbnb.usci.eav.model.base.IBaseContainer;
import kz.bsbnb.usci.eav.model.base.IBaseEntity;
import kz.bsbnb.usci.eav.model.base.IBaseSet;
import kz.bsbnb.usci.eav.model.base.IBaseValue;
import kz.bsbnb.usci.eav.model.base.impl.BaseSet;
import kz.bsbnb.usci.eav.model.base.impl.BaseValueFactory;
import kz.bsbnb.usci.eav.model.meta.IMetaAttribute;
import kz.bsbnb.usci.eav.model.meta.IMetaClass;
import kz.bsbnb.usci.eav.model.meta.IMetaSet;
import kz.bsbnb.usci.eav.model.meta.IMetaType;
import kz.bsbnb.usci.eav.model.meta.impl.MetaContainerTypes;
import kz.bsbnb.usci.eav.model.meta.impl.MetaSet;
import kz.bsbnb.usci.eav.model.persistable.IPersistable;
import kz.bsbnb.usci.eav.persistance.dao.IBaseEntityComplexSetDao;
import kz.bsbnb.usci.eav.persistance.dao.IBaseSetComplexValueDao;
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
public class BaseEntityComplexSetDaoImpl extends JDBCSupport implements IBaseEntityComplexSetDao {
    private final Logger logger = LoggerFactory.getLogger(BaseEntityComplexSetDaoImpl.class);

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    private DSLContext context;

    @Autowired
    IBatchRepository batchRepository;

    @Autowired
    IBaseSetComplexValueDao baseSetComplexValueDao;

    @Override
    public long insert(IPersistable persistable) {
        IBaseValue baseValue = (IBaseValue) persistable;
        BaseSet baseSet = (BaseSet) baseValue.getValue();

        Insert insert = context
                .insertInto(EAV_BE_ENTITY_COMPLEX_SETS)
                .set(EAV_BE_ENTITY_COMPLEX_SETS.ENTITY_ID, baseValue.getBaseContainer().getId())
                .set(EAV_BE_ENTITY_COMPLEX_SETS.ATTRIBUTE_ID, baseValue.getMetaAttribute().getId())
                /*.set(EAV_BE_ENTITY_COMPLEX_SETS.SET_ID, baseSet.getId())*/
                .set(EAV_BE_ENTITY_COMPLEX_SETS.CREDITOR_ID, baseValue.getCreditorId())
                .set(EAV_BE_ENTITY_COMPLEX_SETS.REPORT_DATE, DataUtils.convert(baseValue.getRepDate()))
                .set(EAV_BE_ENTITY_COMPLEX_SETS.IS_CLOSED, DataUtils.convert(baseValue.isClosed()))
                .set(EAV_BE_ENTITY_COMPLEX_SETS.IS_LAST, DataUtils.convert(baseValue.isLast()));

        logger.debug(insert.toString());

        long baseValueId = insertWithId(insert.getSQL(), insert.getBindValues().toArray());

        baseSet.setId(baseValueId);

        return baseValueId;
    }

    @Override
    public void update(IPersistable persistable) {
        IBaseValue baseValue = (IBaseValue) persistable;
        IBaseSet baseSet = (IBaseSet) baseValue.getValue();

        String tableAlias = "cs";
        Update update = context
                .update(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias))
                .set(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).ENTITY_ID, baseValue.getBaseContainer().getId())
                .set(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).ATTRIBUTE_ID, baseValue.getMetaAttribute().getId())
                /*.set(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).SET_ID, baseSet.getId())*/
                .set(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).CREDITOR_ID, baseValue.getCreditorId())
                .set(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).REPORT_DATE, DataUtils.convert(baseValue.getRepDate()))
                .set(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).IS_CLOSED, DataUtils.convert(baseValue.isClosed()))
                .set(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).IS_LAST, DataUtils.convert(baseValue.isLast()))
                .where(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).ID.equal(baseValue.getId()));

        logger.debug(update.toString());

        int count = updateWithStats(update.getSQL(), update.getBindValues().toArray());

        if (count != 1)
            throw new IllegalStateException(Errors.getMessage(Errors.E86, count, baseValue.getId()));
    }

    @Override
    public void delete(IPersistable persistable) {
        String tableAlias = "cs";
        Delete delete = context
                .delete(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias))
                .where(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).ID.equal(persistable.getId()));

        logger.debug(delete.toString());

        int count = updateWithStats(delete.getSQL(), delete.getBindValues().toArray());

        if (count != 1)
            throw new IllegalStateException(Errors.getMessage(Errors.E85 , count, persistable.getId()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public IBaseValue getNextBaseValue(IBaseValue baseValue) {
        IBaseContainer baseContainer = baseValue.getBaseContainer();
        IMetaAttribute metaAttribute = baseValue.getMetaAttribute();

        if (metaAttribute == null)
            throw new IllegalStateException(Errors.getMessage(Errors.E80));

        if (metaAttribute.getId() < 1)
            throw new IllegalStateException(Errors.getMessage(Errors.E81));

        if (baseContainer == null)
            throw new IllegalStateException(Errors.getMessage(Errors.E82, baseValue.getMetaAttribute().getName()));

        if (baseContainer.getId() < 1)
            return null;

        IBaseEntity baseEntity = (IBaseEntity) baseContainer;
        IMetaClass metaClass = baseEntity.getMeta();

        IMetaType metaType = metaAttribute.getMetaType();
        IMetaSet metaSet = (IMetaSet) metaType;

        IBaseValue nextBaseValue = null;

        String tableAlias = "ess";
        String subqueryAlias = "essn";

        Table subqueryTable = context
                .select(DSL.rank().over()
                                .orderBy(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).REPORT_DATE.asc()).as("num_pp"),
                        EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).ID,
                        EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).REPORT_DATE,
                        /*EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).SET_ID,*/
                        EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).IS_CLOSED,
                        EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).IS_LAST)
                .from(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias))
                .where(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).ENTITY_ID.equal(baseEntity.getId()))
                .and(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).ATTRIBUTE_ID.equal(metaAttribute.getId()))
                .and(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).REPORT_DATE.
                        greaterThan(DataUtils.convert(baseValue.getRepDate())))
                .and(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).CREDITOR_ID.equal(baseValue.getCreditorId()))
                .asTable(subqueryAlias);

        Select select = context
                .select(subqueryTable.field(EAV_BE_ENTITY_COMPLEX_SETS.ID),
                        subqueryTable.field(EAV_BE_ENTITY_COMPLEX_SETS.REPORT_DATE),
                        /*subqueryTable.field(EAV_BE_ENTITY_COMPLEX_SETS.SET_ID),*/
                        subqueryTable.field(EAV_BE_ENTITY_COMPLEX_SETS.IS_CLOSED),
                        subqueryTable.field(EAV_BE_ENTITY_COMPLEX_SETS.IS_LAST))
                .from(subqueryTable)
                .where(subqueryTable.field("num_pp").cast(Integer.class).equal(1));


        logger.debug(select.toString());
        List<Map<String, Object>> rows = queryForListWithStats(select.getSQL(), select.getBindValues().toArray());

        if (rows.size() > 1)
            throw new IllegalStateException(Errors.getMessage(Errors.E83, metaAttribute.getName()));

        if (rows.size() == 1) {
            Map<String, Object> row = rows.iterator().next();

            long id = ((BigDecimal) row
                    .get(EAV_BE_ENTITY_COMPLEX_SETS.ID.getName())).longValue();

            boolean closed = ((BigDecimal) row
                    .get(EAV_BE_ENTITY_COMPLEX_SETS.IS_CLOSED.getName())).longValue() == 1;

            boolean last = ((BigDecimal) row
                    .get(EAV_BE_ENTITY_COMPLEX_SETS.IS_LAST.getName())).longValue() == 1;

            /*long setId = ((BigDecimal) row
                    .get(EAV_BE_ENTITY_COMPLEX_SETS.SET_ID.getName())).longValue();*/

            Date reportDate = DataUtils.convertToSQLDate((Timestamp) row
                    .get(EAV_BE_ENTITY_COMPLEX_SETS.REPORT_DATE.getName()));

            IBaseSet baseSet = new BaseSet(id, metaSet.getMemberType());

            baseSetComplexValueDao.loadBaseValues(baseSet, reportDate);

            nextBaseValue = BaseValueFactory.create(
                    metaClass.getType(),
                    metaType,
                    id,
                    baseValue.getCreditorId(),
                    reportDate,
                    baseSet,
                    closed,
                    last);
        }

        return nextBaseValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public IBaseValue getPreviousBaseValue(IBaseValue baseValue) {
        IBaseContainer baseContainer = baseValue.getBaseContainer();
        IMetaAttribute metaAttribute = baseValue.getMetaAttribute();

        if (metaAttribute == null)
            throw new IllegalStateException(Errors.getMessage(Errors.E80));

        if (metaAttribute.getId() < 1)
            throw new IllegalStateException(Errors.getMessage(Errors.E81));

        if (baseContainer == null)
            throw new IllegalStateException(Errors.getMessage(Errors.E82, baseValue.getMetaAttribute().getName()));

        if (baseContainer.getId() < 1)
            return null;

        IBaseEntity baseEntity = (IBaseEntity) baseContainer;
        IMetaClass metaClass = baseEntity.getMeta();

        IMetaType metaType = metaAttribute.getMetaType();
        IMetaSet metaSet = (IMetaSet) metaType;

        IBaseValue previousBaseValue = null;

        String tableAlias = "ess";
        String subqueryAlias = "essn";

        Table subqueryTable = context
                .select(DSL.rank().over()
                                .orderBy(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).REPORT_DATE.desc()).as("num_pp"),
                        EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).ID,
                        EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).REPORT_DATE,
                        /*EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).SET_ID,*/
                        EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).IS_CLOSED,
                        EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).IS_LAST)
                .from(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias))
                .where(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).ENTITY_ID.equal(baseEntity.getId()))
                .and(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).ATTRIBUTE_ID.equal(metaAttribute.getId()))
                .and(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).REPORT_DATE.
                        lessThan(DataUtils.convert(baseValue.getRepDate())))
                .and(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).CREDITOR_ID.equal(baseValue.getCreditorId()))
                .asTable(subqueryAlias);

        Select select = context
                .select(subqueryTable.field(EAV_BE_ENTITY_COMPLEX_SETS.ID),
                        subqueryTable.field(EAV_BE_ENTITY_COMPLEX_SETS.REPORT_DATE),
                        /*subqueryTable.field(EAV_BE_ENTITY_COMPLEX_SETS.SET_ID),*/
                        subqueryTable.field(EAV_BE_ENTITY_COMPLEX_SETS.IS_CLOSED),
                        subqueryTable.field(EAV_BE_ENTITY_COMPLEX_SETS.IS_LAST))
                .from(subqueryTable)
                .where(subqueryTable.field("num_pp").cast(Integer.class).equal(1));


        logger.debug(select.toString());
        List<Map<String, Object>> rows = queryForListWithStats(select.getSQL(), select.getBindValues().toArray());

        if (rows.size() > 1)
            throw new IllegalStateException(Errors.getMessage(Errors.E83, metaAttribute.getName()));

        if (rows.size() >= 1) {
            Map<String, Object> row = rows.iterator().next();

            long id = ((BigDecimal) row
                    .get(EAV_BE_ENTITY_COMPLEX_SETS.ID.getName())).longValue();

            boolean closed = ((BigDecimal) row
                    .get(EAV_BE_ENTITY_COMPLEX_SETS.IS_CLOSED.getName())).longValue() == 1;

            boolean last = ((BigDecimal) row
                    .get(EAV_BE_ENTITY_COMPLEX_SETS.IS_LAST.getName())).longValue() == 1;

            /*long setId = ((BigDecimal) row
                    .get(EAV_BE_ENTITY_COMPLEX_SETS.SET_ID.getName())).longValue();*/

            Date reportDate = DataUtils.convertToSQLDate((Timestamp) row
                    .get(EAV_BE_ENTITY_COMPLEX_SETS.REPORT_DATE.getName()));

            IBaseSet baseSet = new BaseSet(id, metaSet.getMemberType());

            baseSetComplexValueDao.loadBaseValues(baseSet, reportDate);

            previousBaseValue = BaseValueFactory.create(
                    metaClass.getType(),
                    metaType,
                    id,
                    baseValue.getCreditorId(),
                    reportDate,
                    baseSet,
                    closed,
                    last);
        }

        return previousBaseValue;
    }

    @Override
    public IBaseValue getClosedBaseValue(IBaseValue baseValue) {
        IBaseContainer baseContainer = baseValue.getBaseContainer();
        IMetaAttribute metaAttribute = baseValue.getMetaAttribute();

        if (metaAttribute == null)
            throw new IllegalStateException(Errors.getMessage(Errors.E80));

        if (metaAttribute.getId() < 1)
            throw new IllegalStateException(Errors.getMessage(Errors.E81));

        if (baseContainer == null)
            throw new IllegalStateException(Errors.getMessage(Errors.E82, baseValue.getMetaAttribute().getName()));

        if (baseContainer.getId() < 1)
            return null;

        IMetaType metaType = metaAttribute.getMetaType();
        IMetaSet metaSet = (IMetaSet) metaType;

        IBaseValue closedBaseValue = null;

        String tableAlias = "ess";
        Select select = context
                .select(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).ID,
                        /*EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).SET_ID,*/
                        EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).REPORT_DATE,
                        EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).IS_LAST)
                .from(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias))
                .where(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).ENTITY_ID.equal(baseContainer.getId()))
                .and(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).ATTRIBUTE_ID.equal(metaAttribute.getId()))
                .and(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).REPORT_DATE.
                        lessOrEqual(DataUtils.convert(baseValue.getRepDate())))
                .and(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).CREDITOR_ID.equal(baseValue.getCreditorId()))
                .and(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).IS_CLOSED.equal(DataUtils.convert(true)));

        logger.debug(select.toString());
        List<Map<String, Object>> rows = queryForListWithStats(select.getSQL(), select.getBindValues().toArray());

        if (rows.size() > 1)
            throw new IllegalStateException(Errors.getMessage(Errors.E83, metaAttribute.getName()));

        if (rows.size() == 1) {
            Map<String, Object> row = rows.iterator().next();

            long id = ((BigDecimal) row
                    .get(EAV_BE_ENTITY_COMPLEX_SETS.ID.getName())).longValue();

            boolean last = ((BigDecimal) row
                    .get(EAV_BE_ENTITY_COMPLEX_SETS.IS_LAST.getName())).longValue() == 1;

            /*long setId = ((BigDecimal) row
                    .get(EAV_BE_ENTITY_COMPLEX_SETS.SET_ID.getName())).longValue();*/

            Date reportDate = DataUtils.convertToSQLDate((Timestamp) row
                    .get(EAV_BE_ENTITY_COMPLEX_SETS.REPORT_DATE.getName()));

            IBaseSet baseSet = new BaseSet(id, metaSet.getMemberType());

            baseSetComplexValueDao.loadBaseValues(baseSet, reportDate);

            closedBaseValue = BaseValueFactory.create(
                    MetaContainerTypes.META_CLASS,
                    metaType,
                    id,
                    baseValue.getCreditorId(),
                    reportDate,
                    baseSet,
                    true,
                    last);
        }

        return closedBaseValue;
    }

    @Override
    public IBaseValue getLastBaseValue(IBaseValue baseValue) {
        IBaseContainer baseContainer = baseValue.getBaseContainer();
        IMetaAttribute metaAttribute = baseValue.getMetaAttribute();

        if (metaAttribute == null)
            throw new IllegalStateException(Errors.getMessage(Errors.E80));

        if (metaAttribute.getId() < 1)
            throw new IllegalStateException(Errors.getMessage(Errors.E81));

        if (baseContainer == null)
            throw new IllegalStateException(Errors.getMessage(Errors.E82, baseValue.getMetaAttribute().getName()));

        if (baseContainer.getId() < 1)
            return null;

        IMetaType metaType = metaAttribute.getMetaType();
        IMetaSet metaSet = (IMetaSet) metaType;

        IBaseValue lastBaseValue = null;

        String tableAlias = "ess";
        Select select = context
                .select(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).ID,
                        EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).REPORT_DATE,
                        /*EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).SET_ID,*/
                        EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).IS_LAST)
                .from(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias))
                .where(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).ENTITY_ID.equal(baseContainer.getId()))
                .and(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).ATTRIBUTE_ID.equal(metaAttribute.getId()))
                .and(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).CREDITOR_ID.equal(baseValue.getCreditorId()))
                .and(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).IS_LAST.equal(DataUtils.convert(true)));

        logger.debug(select.toString());
        List<Map<String, Object>> rows = queryForListWithStats(select.getSQL(), select.getBindValues().toArray());

        if (rows.size() > 1)
            throw new IllegalStateException(Errors.getMessage(Errors.E83, metaAttribute.getName()));

        if (rows.size() == 1) {
            Map<String, Object> row = rows.iterator().next();

            long id = ((BigDecimal) row
                    .get(EAV_BE_ENTITY_COMPLEX_SETS.ID.getName())).longValue();

            boolean closed = ((BigDecimal) row
                    .get(EAV_BE_ENTITY_COMPLEX_SETS.IS_CLOSED.getName())).longValue() == 1;

            /*long setId = ((BigDecimal) row
                    .get(EAV_BE_ENTITY_COMPLEX_SETS.SET_ID.getName())).longValue();*/

            Date reportDate = DataUtils.convertToSQLDate((Timestamp) row
                    .get(EAV_BE_ENTITY_COMPLEX_SETS.REPORT_DATE.getName()));

            IBaseSet baseSet = new BaseSet(id, metaSet.getMemberType());
            baseSetComplexValueDao.loadBaseValues(baseSet, baseValue.getRepDate());

            lastBaseValue = BaseValueFactory.create(
                    MetaContainerTypes.META_CLASS,
                    metaType,
                    id,
                    baseValue.getCreditorId(),
                    reportDate,
                    baseSet,
                    closed,
                    true);
        }

        return lastBaseValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void loadBaseValues(IBaseEntity baseEntity, Date actualReportDate) {
        Table tableOfComplexSets = EAV_M_COMPLEX_SET.as("cs");
        Table tableOfEntityComplexSets = EAV_BE_ENTITY_COMPLEX_SETS.as("ecs");

        Select select;

        Table tableNumbering = context
                .select(DSL.rank().over()
                                .partitionBy(tableOfEntityComplexSets.field(EAV_BE_ENTITY_COMPLEX_SETS.ATTRIBUTE_ID))
                                .orderBy(tableOfEntityComplexSets.field(EAV_BE_ENTITY_COMPLEX_SETS.REPORT_DATE)).as("num_pp"),
                        tableOfEntityComplexSets.field(EAV_BE_ENTITY_COMPLEX_SETS.ID),
                        tableOfEntityComplexSets.field(EAV_BE_ENTITY_COMPLEX_SETS.CREDITOR_ID),
                        tableOfEntityComplexSets.field(EAV_BE_ENTITY_COMPLEX_SETS.ATTRIBUTE_ID),
                        tableOfEntityComplexSets.field(EAV_BE_ENTITY_COMPLEX_SETS.REPORT_DATE),
                        /*tableOfEntityComplexSets.field(EAV_BE_ENTITY_COMPLEX_SETS.SET_ID),*/
                        tableOfEntityComplexSets.field(EAV_BE_ENTITY_COMPLEX_SETS.IS_CLOSED),
                        tableOfEntityComplexSets.field(EAV_BE_ENTITY_COMPLEX_SETS.IS_LAST))
                .from(tableOfEntityComplexSets)
                .where(tableOfEntityComplexSets.field(EAV_BE_ENTITY_COMPLEX_SETS.ENTITY_ID).eq(baseEntity.getId()))
                .and(tableOfEntityComplexSets.field(EAV_BE_ENTITY_COMPLEX_SETS.REPORT_DATE)
                        .lessOrEqual(DataUtils.convert(actualReportDate)))
                .asTable("essn");

        select = context
                .select(tableOfComplexSets.field(EAV_M_COMPLEX_SET.NAME),
                        tableNumbering.field(EAV_BE_ENTITY_COMPLEX_SETS.ID),
                        tableNumbering.field(EAV_BE_ENTITY_COMPLEX_SETS.REPORT_DATE),
                        tableNumbering.field(EAV_BE_ENTITY_COMPLEX_SETS.CREDITOR_ID),
                        /*tableNumbering.field(EAV_BE_ENTITY_COMPLEX_SETS.SET_ID),*/
                        tableNumbering.field(EAV_BE_ENTITY_COMPLEX_SETS.IS_CLOSED),
                        tableNumbering.field(EAV_BE_ENTITY_COMPLEX_SETS.IS_LAST))
                .from(tableNumbering)
                .join(tableOfComplexSets)
                .on(tableNumbering.field(EAV_BE_ENTITY_COMPLEX_SETS.ATTRIBUTE_ID)
                        .eq(tableOfComplexSets.field(EAV_M_COMPLEX_SET.ID)))
                .where(tableNumbering.field("num_pp").cast(Integer.class).equal(1))
                .and((tableNumbering.field(EAV_BE_ENTITY_COMPLEX_SETS.IS_CLOSED).equal(false)
                        .and(tableOfComplexSets.field(EAV_M_COMPLEX_SET.IS_FINAL).equal(false)))
                        .or(tableNumbering.field(EAV_BE_ENTITY_COMPLEX_SETS.REPORT_DATE).equal(actualReportDate)
                                .and(tableOfComplexSets.field(EAV_M_COMPLEX_SET.IS_FINAL).equal(true))));

        logger.debug(select.toString());
        List<Map<String, Object>> rows = queryForListWithStats(select.getSQL(), select.getBindValues().toArray());

        for (Map<String, Object> row : rows) {
            String attribute = (String) row.get(EAV_M_COMPLEX_SET.NAME.getName());

            long id = ((BigDecimal) row.get(EAV_BE_ENTITY_COMPLEX_SETS.ID.getName())).longValue();

            long creditorId = ((BigDecimal) row.get(EAV_BE_ENTITY_COMPLEX_SETS.CREDITOR_ID.getName())).longValue();

            long baseValueId = ((BigDecimal) row.get(EAV_BE_ENTITY_COMPLEX_SETS.ID.getName())).longValue();

            Date reportDate = DataUtils.convertToSQLDate((Timestamp)
                    row.get(EAV_BE_ENTITY_COMPLEX_SETS.REPORT_DATE.getName()));

            IMetaType metaType = baseEntity.getMemberType(attribute);
            IMetaSet metaSet = (MetaSet) metaType;
            IMetaType metaSetMemberType = metaSet.getMemberType();
            IBaseSet baseSet = new BaseSet(id, metaSetMemberType);

            if (metaSetMemberType.isSet())
                throw new UnsupportedOperationException(Errors.getMessage(Errors.E2));

            baseSetComplexValueDao.loadBaseValues(baseSet, actualReportDate);

            baseEntity.put(attribute, BaseValueFactory.create(
                    MetaContainerTypes.META_CLASS,
                    metaType,
                    baseValueId,
                    creditorId,
                    reportDate,
                    baseSet,
                    false,
                    true));
        }
    }

    @Override
    public void deleteAll(long baseEntityId) {
        Set<Long> childBaseSetIds = getChildBaseSetIds(baseEntityId);

        String tableAlias = "cv";
        Delete delete = context
                .delete(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias))
                .where(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).ENTITY_ID.equal(baseEntityId));

        logger.debug(delete.toString());
        updateWithStats(delete.getSQL(), delete.getBindValues().toArray());
    }

    @Override
    public Set<Long> getChildBaseSetIds(long parentBaseEntityId) {
        Set<Long> baseSetIds = new HashSet<>();

        String tableAlias = "bv";
        Select select = context
                .select(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).ID)
                .from(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias))
                .where(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).ENTITY_ID.equal(parentBaseEntityId))
                .groupBy(EAV_BE_ENTITY_COMPLEX_SETS.as(tableAlias).ID);

        logger.debug(select.toString());
        List<Map<String, Object>> rows = queryForListWithStats(select.getSQL(), select.getBindValues().toArray());

        if (rows.size() > 0) {
            for (Map<String, Object> row : rows) {
                long childBaseSetId = ((BigDecimal) row.get(EAV_BE_ENTITY_COMPLEX_SETS.ID.getName())).longValue();
                baseSetIds.add(childBaseSetId);
            }
        }

        return baseSetIds;
    }

    @Override
    public Set<Long> getChildBaseEntityIdsWithoutRefs(long parentBaseEntityId) {
        Set<Long> baseEntityIds = new HashSet<>();

        String entitiesTableAlias = "e";
        String classesTableAlias = "c";
        String entityComplexSetsTableAlias = "ecs";
        String complexSetValuesTableAlias = "csv";
        Select select = context
                .select(EAV_BE_ENTITIES.as(entitiesTableAlias).ID)
                .from(EAV_BE_ENTITIES.as(entitiesTableAlias))
                .join(EAV_M_CLASSES.as(classesTableAlias))
                .on(EAV_BE_ENTITIES.as(entitiesTableAlias).CLASS_ID.equal(EAV_M_CLASSES.as(classesTableAlias).ID))
                .join(EAV_BE_COMPLEX_SET_VALUES.as(complexSetValuesTableAlias))
                .on(EAV_BE_COMPLEX_SET_VALUES.as(complexSetValuesTableAlias).ENTITY_VALUE_ID
                        .equal(EAV_BE_ENTITIES.as(entitiesTableAlias).ID))
                .join(EAV_BE_ENTITY_COMPLEX_SETS.as(entityComplexSetsTableAlias))
                .on(EAV_BE_ENTITY_COMPLEX_SETS.as(entityComplexSetsTableAlias).ID
                        .equal(EAV_BE_COMPLEX_SET_VALUES.as(complexSetValuesTableAlias).SET_ID))
                .where(EAV_BE_ENTITY_COMPLEX_SETS.as(entityComplexSetsTableAlias).ENTITY_ID.equal(parentBaseEntityId))
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
}

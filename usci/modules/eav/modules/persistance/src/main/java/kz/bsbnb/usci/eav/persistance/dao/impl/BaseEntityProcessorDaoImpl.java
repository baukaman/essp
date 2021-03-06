package kz.bsbnb.usci.eav.persistance.dao.impl;

import kz.bsbnb.usci.eav.StaticRouter;
import kz.bsbnb.usci.eav.manager.IEAVLoggerDao;
import kz.bsbnb.usci.eav.manager.IBaseEntityManager;
import kz.bsbnb.usci.eav.manager.impl.BaseEntityManager;
import kz.bsbnb.usci.eav.model.base.IBaseEntity;
import kz.bsbnb.usci.eav.model.base.IBaseEntityReportDate;
import kz.bsbnb.usci.eav.model.base.IBaseSet;
import kz.bsbnb.usci.eav.model.base.IBaseValue;
import kz.bsbnb.usci.eav.model.base.impl.BaseEntity;
import kz.bsbnb.usci.eav.model.base.impl.BaseEntityReportDate;
import kz.bsbnb.usci.eav.model.base.impl.OperationType;
import kz.bsbnb.usci.eav.model.exceptions.KnownException;
import kz.bsbnb.usci.eav.model.exceptions.KnownIterativeException;
import kz.bsbnb.usci.eav.model.meta.IMetaAttribute;
import kz.bsbnb.usci.eav.model.meta.IMetaType;
import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;
import kz.bsbnb.usci.eav.persistance.dao.*;
import kz.bsbnb.usci.eav.persistance.dao.listener.IDaoListener;
import kz.bsbnb.usci.eav.persistance.dao.pool.IPersistableDaoPool;
import kz.bsbnb.usci.eav.persistance.db.JDBCSupport;
import kz.bsbnb.usci.eav.persistance.searcher.IBaseEntitySearcher;
import kz.bsbnb.usci.eav.persistance.searcher.pool.impl.BasicBaseEntitySearcherPool;
import kz.bsbnb.usci.eav.repository.IRefRepository;
import kz.bsbnb.usci.eav.rule.impl.RulesSingleton;
import kz.bsbnb.usci.eav.tool.optimizer.impl.BasicOptimizer;
import kz.bsbnb.usci.eav.util.Errors;
import org.jooq.DSLContext;
import org.jooq.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

import static kz.bsbnb.eav.persistance.generated.Tables.*;

@Repository
@Primary
public class BaseEntityProcessorDaoImpl extends JDBCSupport implements IBaseEntityProcessorDao {
    private final Logger logger = LoggerFactory.getLogger(BaseEntityProcessorDaoImpl.class);

    private static final String LOGIC_RULE_SETTING = "LOGIC_RULE_SETTING";
    private static final String LOGIC_RULE_META = "LOGIC_RULE_META";
    private static final String BVUNO_SETTING = "BVUNO_SETTING";
    private static final String BVUNO_IDS = "BVUNO_IDS";

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    private DSLContext context;

    @Autowired
    private IPersistableDaoPool persistableDaoPool;

    @Autowired
    private IBaseEntityLoadDao baseEntityLoadDao;

    @Autowired
    private IBaseEntityDao baseEntityDao;

    @Autowired
    private IBaseEntityApplyDao baseEntityApplyDao;

    @Autowired
    private IEavOptimizerDao eavOptimizerDao;

    @Autowired
    private IReportDao reportDao;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

    @Autowired
    private BasicBaseEntitySearcherPool searcherPool;

    @Autowired
    private RulesSingleton rulesSingleton;

    @Autowired
    private IEavGlobalDao globalDao;

    private IDaoListener applyListener;

    private Set<String> metaRules;

    @Autowired
    private IRefRepository refRepository;

    private Set bvunoSet;

    @Autowired
    private IEAVLoggerDao eavLoggerDao;

    public void setApplyListener(IDaoListener applyListener) {
        this.applyListener = applyListener;
    }

    public boolean dbApplyEnabled;

    public void setDbApplyEnabled(boolean dbApplyEnabled) {
        this.dbApplyEnabled = dbApplyEnabled;
    }

    @Override
    public long search(IBaseEntity baseEntity, long creditorId) {
        IBaseEntitySearcher searcher =searcherPool.getSearcher(baseEntity.getMeta().getClassName());
        Long baseEntityId = searcher.findSingle((BaseEntity) baseEntity, creditorId);
        return baseEntityId == null ? 0 : baseEntityId;
    }

    @Override
    public IBaseEntity prepare(final IBaseEntity baseEntity, long creditorId) {
        MetaClass metaClass = baseEntity.getMeta();

        final boolean isReference = metaClass.isReference();
        creditorId = isReference ? 0 : creditorId;

        baseEntity.getBaseEntityReportDate().setCreditorId(creditorId);

        for (String attrName : baseEntity.getAttributes()) {
            IMetaAttribute metaAttribute = baseEntity.getMeta().getMetaAttribute(attrName);
            IMetaType metaType = baseEntity.getMemberType(attrName);
            IBaseValue baseValue = baseEntity.getBaseValue(attrName);

            if (!metaAttribute.isKey())
                continue;

            if (metaType.isComplex()) {
                if (baseValue.getValue() != null) {
                    if (metaType.isSet()) {
                        IBaseSet childBaseSet = (IBaseSet) baseValue.getValue();
                        childBaseSet.setCreditorId(creditorId);

                        for (IBaseValue childBaseValue : childBaseSet.get()) {
                            IBaseEntity childBaseEntity = (IBaseEntity) childBaseValue.getValue();

                            if (childBaseEntity.getValueCount() != 0)
                                prepare((IBaseEntity) childBaseValue.getValue(), creditorId);
                        }
                    } else {
                        IBaseEntity childBaseEntity = (IBaseEntity) baseValue.getValue();

                        if (childBaseEntity.getValueCount() != 0)
                            prepare(childBaseEntity, creditorId);
                    }
                }
            }

            baseValue.setCreditorId(creditorId);
        }

        if (metaClass.isReference()) {
            long refId = refRepository.prepareRef(baseEntity);

            if (refId > 0)
                baseEntity.setId(refId);
        } else {
            if (metaClass.isSearchable() && baseEntity.getId() == 0) {
                long baseEntityId;

                if (BasicOptimizer.metaList.contains(metaClass.getClassName())) {
                    baseEntityId = eavOptimizerDao.find(creditorId, baseEntity.getMeta().getId(), BasicOptimizer.getKeyString(baseEntity));
                } else {
                    baseEntityId = search(baseEntity, creditorId);
                }

                if (baseEntityId > 0)
                    baseEntity.setId(baseEntityId);
            }

            if(metaClass.parentIsKey() && baseEntity.getId() == 0) {
                baseEntity.setId(search(baseEntity, creditorId));
            }
        }

        for (String attrName : baseEntity.getAttributes()) {
            IMetaAttribute metaAttribute = baseEntity.getMeta().getMetaAttribute(attrName);
            IMetaType metaType = baseEntity.getMemberType(attrName);
            IBaseValue baseValue = baseEntity.getBaseValue(attrName);

            if (metaAttribute.isKey())
                continue;

            if (metaType.isComplex()) {
                if (baseValue.getValue() != null) {
                    if (metaType.isSet()) {
                        IBaseSet childBaseSet = (IBaseSet) baseValue.getValue();
                        childBaseSet.setCreditorId(creditorId);

                        for (IBaseValue childBaseValue : childBaseSet.get()) {
                            IBaseEntity childBaseEntity = (IBaseEntity) childBaseValue.getValue();

                            if (childBaseEntity.getValueCount() != 0) {
                                childBaseEntity.setAddInfo(baseEntity, true, metaAttribute.getId());

                                prepare((IBaseEntity) childBaseValue.getValue(), creditorId);
                            }
                        }
                    } else {
                        IBaseEntity childBaseEntity = (IBaseEntity) baseValue.getValue();

                        if (childBaseEntity.getValueCount() != 0) {
                            childBaseEntity.setAddInfo(baseEntity, false, metaAttribute.getId());

                            prepare(childBaseEntity, creditorId);
                        }
                    }
                }
            }

            baseValue.setCreditorId(creditorId);
        }

        return baseEntity;
    }

    boolean rulesEnabledForUser(IBaseEntity baseEntity){
        if(!StaticRouter.rulesEnabled())
            return false;
        return baseEntity.getUserId() == null || baseEntity.getUserId() != 100500L;
    }

    private void checkForRules(BaseEntity baseEntity) {
        long ruleTime = System.currentTimeMillis();
        try {
            if (metaRules == null) {
                String[] metaArray = globalDao.getValue(LOGIC_RULE_SETTING, LOGIC_RULE_META).split(",");
                metaRules = new HashSet<>(Arrays.asList(metaArray));
            }

            if (rulesEnabledForUser(baseEntity) && metaRules.contains(baseEntity.getMeta().getClassName())) {
                List<String> errors = new ArrayList<>();
                try {
                    rulesSingleton.runRules(baseEntity, baseEntity.getMeta().getClassName() + "_parser", baseEntity.getReportDate());
                    for (String s : baseEntity.getValidationErrors()) errors.add(s);
                } catch (Exception e) {
                    logger.error(Errors.compose(Errors.E290, e));
                    throw new RuntimeException(Errors.compose(Errors.E290, e));
                }

                if (errors.size() > 0)
                    throw new KnownIterativeException(errors);
            }
        } finally {
            sqlStats.put("checkForRules", (System.currentTimeMillis() - ruleTime));
        }
    }

    @Override
    @Transactional
    public IBaseEntity process(final IBaseEntity baseEntity) {
        IBaseEntityManager baseEntityManager = new BaseEntityManager();
        baseEntityManager.setDeleteLogger(eavLoggerDao);

        IBaseEntity baseEntityPostPrepared;
        IBaseEntity baseEntityApplied;

        /* Все данные кроме справочников должны иметь кредитора */
        if (!baseEntity.getMeta().isReference() && baseEntity.getBaseEntityReportDate().getCreditorId() == 0)
            throw new IllegalStateException(Errors.compose(Errors.E197));

        long creditorId = baseEntity.getBaseEntityReportDate().getCreditorId();
        baseEntityManager.registerCreditorId(creditorId);

        long prepareTime = System.currentTimeMillis();
        baseEntityPostPrepared = prepare(((BaseEntity) baseEntity).clone(), creditorId);
        sqlStats.put("java::prepare", (System.currentTimeMillis() - prepareTime));

        /* Проверка сущности на бизнес правила */
        checkForRules((BaseEntity) baseEntityPostPrepared);

        if (baseEntityPostPrepared.getOperation() != null) {
            switch (baseEntityPostPrepared.getOperation()) {
                case DELETE:
                    if (baseEntityPostPrepared.getId() <= 0)
                        throw new KnownException(Errors.compose(Errors.E112));

                    if (baseEntityDao.isUsed(baseEntityPostPrepared.getId()))
                        throw new KnownException(Errors.compose(Errors.E292));

                    baseEntityManager.registerAsDeleted(baseEntityPostPrepared);
                    baseEntityApplied = baseEntityLoadDao.loadByMaxReportDate(baseEntityPostPrepared.getId(), baseEntityPostPrepared.getReportDate());
                    baseEntityApplied.setOperation(OperationType.DELETE);

                    baseEntityApplyDao.applyToDb(baseEntityManager);
                    break;
                case CLOSE:
                    if (baseEntityPostPrepared.getId() <= 0)
                        throw new KnownException(Errors.compose(Errors.E114));

                    IBaseEntityReportDateDao baseEntityReportDateDao = persistableDaoPool.getPersistableDao(
                            BaseEntityReportDate.class, IBaseEntityReportDateDao.class);

                    Date minReportDate = baseEntityReportDateDao.getMinReportDate(baseEntityPostPrepared.getId());
                    if (minReportDate.compareTo(baseEntityPostPrepared.getReportDate()) >= 0)
                        throw new IllegalStateException(Errors.compose(Errors.E115));

                    boolean reportDateExists = baseEntityReportDateDao.exists(baseEntityPostPrepared.getId(), baseEntityPostPrepared.getReportDate());

                    IBaseEntityReportDate baseEntityReportDate;

                    if (reportDateExists) {
                        baseEntityReportDate = baseEntityReportDateDao.load(baseEntityPostPrepared.getId(),
                                baseEntityPostPrepared.getReportDate());

                        baseEntityReportDate.setBaseEntity(baseEntityPostPrepared);
                    } else {
                        baseEntityReportDate = baseEntityPostPrepared.getBaseEntityReportDate();
                        baseEntityPostPrepared.calculateValueCount(null);
                    }

                    baseEntityReportDate.setClosed(true);

                    if (reportDateExists) {
                        baseEntityManager.registerAsUpdated(baseEntityReportDate);
                    } else {
                        baseEntityManager.registerAsInserted(baseEntityReportDate);
                    }

                    baseEntityApplied = ((BaseEntity) baseEntityPostPrepared).clone();
                    baseEntityApplyDao.applyToDb(baseEntityManager);
                    break;
                case OPEN:
                    if (baseEntityPostPrepared.getId() <= 0)
                        throw new KnownException(Errors.compose(Errors.E114));

                    IBaseEntityReportDateDao berdDao = persistableDaoPool.getPersistableDao(
                            BaseEntityReportDate.class, IBaseEntityReportDateDao.class);

                    Date minRepDate = berdDao.getMinReportDate(baseEntityPostPrepared.getId());
                    if (minRepDate.compareTo(baseEntityPostPrepared.getReportDate()) >= 0)
                        throw new IllegalStateException(Errors.compose(Errors.E115));

                    boolean repDateExists = berdDao.exists(baseEntityPostPrepared.getId(), baseEntityPostPrepared.getReportDate());

                    IBaseEntityReportDate baseEntityRepDate;

                    if (repDateExists) {
                        baseEntityRepDate = berdDao.load(baseEntityPostPrepared.getId(),
                                baseEntityPostPrepared.getReportDate());

                        baseEntityRepDate.setBaseEntity(baseEntityPostPrepared);
                    } else {
                        baseEntityRepDate = baseEntityPostPrepared.getBaseEntityReportDate();
                        baseEntityPostPrepared.calculateValueCount(null);
                    }

                    baseEntityRepDate.setClosed(false);

                    if (repDateExists) {
                        baseEntityManager.registerAsUpdated(baseEntityRepDate);
                    } else {
                        baseEntityManager.registerAsInserted(baseEntityRepDate);
                    }

                    baseEntityApplied = ((BaseEntity) baseEntityPostPrepared).clone();
                    baseEntityApplyDao.applyToDb(baseEntityManager);
                    break;
                case CHECKED_REMOVE:
                    if (baseEntityPostPrepared.getId() <= 0)
                        throw new KnownException(Errors.compose(Errors.E112));

                    if (baseEntityDao.isUsed(baseEntityPostPrepared.getId()))
                        throw new KnownException(Errors.compose(Errors.E292));

                    Date lastApprovedDate = reportDao.getLastApprovedDate(creditorId);
                    if(lastApprovedDate != null) {
                        IBaseEntity creditor = baseEntityLoadDao.load(creditorId);
                        Integer period = (Integer) creditor.getEl("subject_type.report_period_duration_months");
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(lastApprovedDate);
                        cal.add(Calendar.MONTH, period == null ? 1 : period);

                        if (!baseEntity.getReportDate().equals(cal.getTime()))
                            throw new KnownException(Errors.compose(Errors.E296, convertToDate(cal.getTime()), convertToDate(baseEntity.getReportDate())));

                        baseEntityReportDateDao = persistableDaoPool.getPersistableDao(
                                BaseEntityReportDate.class, IBaseEntityReportDateDao.class);

                        Date previousReportDate = baseEntityReportDateDao.getPreviousReportDate(baseEntityPostPrepared.getId(), baseEntity.getReportDate());
                        if (previousReportDate != null)
                            throw new RuntimeException(Errors.compose(Errors.E297, convertToDate(previousReportDate)));
                    }

                    baseEntityManager.registerAsDeleted(baseEntityPostPrepared);
                    baseEntityApplied = baseEntityLoadDao.loadByMaxReportDate(baseEntityPostPrepared.getId(), baseEntityPostPrepared.getReportDate());
                    baseEntityApplied.setOperation(OperationType.DELETE);
                    eavOptimizerDao.delete(baseEntityApplied.getId());
                    baseEntityApplyDao.applyToDb(baseEntityManager);

                    break;
                case INSERT:
                    if (baseEntityPostPrepared.getId() > 0)
                        throw new KnownException(Errors.compose(Errors.E196, baseEntityPostPrepared.getId()));

                    long applyTime = System.currentTimeMillis();
                    baseEntityApplied = baseEntityApplyDao.apply(creditorId, baseEntityPostPrepared, null, baseEntityManager);
                    sqlStats.put("java::apply", (System.currentTimeMillis() - applyTime));

                    if (rulesEnabledForUser(baseEntity))
                        processLogicControl(baseEntityApplied);

                    baseEntityApplyDao.applyToDb(baseEntityManager);
                    baseEntityApplied.setOperation(OperationType.INSERT);
                    break;
                case UPDATE:
                    if (baseEntityPostPrepared.getId() <= 0)
                        throw new KnownException(Errors.compose(Errors.E198));

                    applyTime = System.currentTimeMillis();
                    baseEntityApplied = baseEntityApplyDao.apply(creditorId, baseEntityPostPrepared, null, baseEntityManager);
                    sqlStats.put("java::apply", (System.currentTimeMillis() - applyTime));

                    if (rulesEnabledForUser(baseEntity)) {
                        processLogicControl(baseEntityApplied);
                    }

                    baseEntityApplyDao.applyToDb(baseEntityManager);
                    baseEntityApplied.setOperation(OperationType.UPDATE);
                    break;
                default:
                    throw new UnsupportedOperationException(Errors.compose(Errors.E118, baseEntityPostPrepared.getOperation()));
            }
        } else {
            long applyTime = System.currentTimeMillis();
            baseEntityApplied = baseEntityApplyDao.apply(creditorId, baseEntityPostPrepared, null, baseEntityManager);
            sqlStats.put("java::apply", (System.currentTimeMillis() - applyTime));

            if (rulesEnabledForUser(baseEntity)) {
                processLogicControl(baseEntityApplied);
            }

            if(dbApplyEnabled)
                baseEntityApplyDao.applyToDb(baseEntityManager);
        }

        if (applyListener != null)
            applyListener.applyToDBEnded(baseEntityApplied);

        if (baseEntityApplied.getMeta().isReference())
            refRepository.installRef(baseEntityApplied);

        return baseEntityApplied;
    }

    private boolean isBvuno(long creditorId) {

        if(bvunoSet == null || bvunoSet.size() < 1) {
            try {
                bvunoSet = new HashSet();
                String[] str = globalDao.get(BVUNO_SETTING, BVUNO_IDS).getValue().split(",");

                for (String s : str) {
                    bvunoSet.add(Long.parseLong(s));
                }
            } catch (Exception e) {
                logger.error("bvuno map is empty !!! " + e.getMessage());
            }
        }

        return bvunoSet.contains(creditorId);
    }

    private String convertToDate(Date date){
        synchronized (dateFormat) {
            return dateFormat.format(date);
        }
    }

    private void tempLogicControlUpdate(IBaseEntity baseEntityApplied) {
        if (metaRules == null) {
            String[] metaArray = globalDao.getValue(LOGIC_RULE_SETTING, LOGIC_RULE_META).split(",");
            metaRules = new HashSet<>(Arrays.asList(metaArray));
        }

        if (metaRules.contains(baseEntityApplied.getMeta().getClassName())) {
            rulesSingleton.runRules(baseEntityApplied, "dev_process",
                    baseEntityApplied.getReportDate());

            if (baseEntityApplied.getValidationErrors().size() > 0)
                throw new KnownIterativeException(baseEntityApplied.getValidationErrors());
        }

    }

    private void processLogicControl(IBaseEntity baseEntityApplied) {
        long ruleTime = System.currentTimeMillis();

        try {
            if (metaRules == null) {
                String[] metaArray = globalDao.getValue(LOGIC_RULE_SETTING, LOGIC_RULE_META).split(",");
                metaRules = new HashSet<>(Arrays.asList(metaArray));
            }

            if (metaRules.contains(baseEntityApplied.getMeta().getClassName())) {
                rulesSingleton.runRules(baseEntityApplied, baseEntityApplied.getMeta().getClassName() + "_process",
                        baseEntityApplied.getReportDate());

                if (baseEntityApplied.getValidationErrors().size() > 0)
                    throw new KnownIterativeException(baseEntityApplied.getValidationErrors());
            }
        } finally {
            sqlStats.put("processLogicControl", (System.currentTimeMillis() - ruleTime));
        }
    }

    public List<Long> getEntityIDsByMetaClass(long metaClassId) {
        ArrayList<Long> entityIds = new ArrayList<>();

        Select select = context
                .select(EAV_BE_ENTITIES.ID)
                .from(EAV_BE_ENTITIES)
                .where(EAV_BE_ENTITIES.CLASS_ID.equal(metaClassId));

        logger.debug(select.toString());
        List<Map<String, Object>> rows = queryForListWithStats(select.getSQL(), select.getBindValues().toArray());

        for (Map<String, Object> row : rows)
            entityIds.add(((BigDecimal) row.get(EAV_BE_ENTITIES.ID.getName())).longValue());

        return entityIds;
    }

    public List<BaseEntity> getEntityByMetaClass(MetaClass meta) {
        List<Long> ids = getEntityIDsByMetaClass(meta.getId());

        ArrayList<BaseEntity> entities = new ArrayList<>();

        for (Long id : ids)
            entities.add((BaseEntity) baseEntityLoadDao.load(id));

        return entities;
    }

    @Override
    public boolean isApproved(long creditorId) {
        Select select = context
                .select(EAV_A_CREDITOR_STATE.ID)
                .from(EAV_A_CREDITOR_STATE)
                .where(EAV_A_CREDITOR_STATE.CREDITOR_ID.equal(creditorId));

        logger.debug(select.toString());
        List<Map<String, Object>> rows = queryForListWithStats(select.getSQL(), select.getBindValues().toArray());

        return rows.size() > 1;
    }

    @Override
    @Transactional
    public boolean remove(long baseEntityId) {
        IBaseEntityDao baseEntityDao = persistableDaoPool.getPersistableDao(BaseEntity.class, IBaseEntityDao.class);
        return baseEntityDao.deleteRecursive(baseEntityId);
    }

    public IBaseEntityLoadDao getBaseEntityLoadDao() {
        return baseEntityLoadDao;
    }

    @Override
    public void prepareClosedDates(IBaseEntity baseEntity, long creditorId) {
        for(String attrName: baseEntity.getAttributes()) {
            IMetaAttribute metaAttribute = baseEntity.getMeta().getMetaAttribute(attrName);
            IMetaType metaType = baseEntity.getMemberType(attrName);
            IBaseValue baseValue = baseEntity.getBaseValue(attrName);

            IBaseValueDao valueDao = persistableDaoPool.getPersistableDao(baseValue.getClass(), IBaseValueDao.class);

            IBaseValue baseValueClosed = valueDao.getNextBaseValue(baseValue);

            if(baseValueClosed == null)
                baseValueClosed = valueDao.getClosedBaseValue(baseValue);

            if(baseValueClosed != null) {
                baseValue.setCloseDate(baseValueClosed.getRepDate());
            }

            if(metaType.isComplex()) {
                if(metaType.isSet()) {
                    IBaseSet childBaseSet = (IBaseSet) baseValue.getValue();

                    for(IBaseValue childBaseValue : childBaseSet.get()) {
                        IBaseSetValueDao setValueDao = persistableDaoPool
                                .getPersistableDao(childBaseValue.getClass(), IBaseSetValueDao.class);

                        IBaseValue childBaseValueClosed = setValueDao.getNextBaseValue(childBaseValue);

                        if(childBaseValueClosed == null)
                            childBaseValueClosed = setValueDao.getClosedBaseValue(childBaseValue);

                        if(childBaseValueClosed != null) {
                            childBaseValue.setCloseDate(childBaseValueClosed.getRepDate());
                        }

                        prepareClosedDates(((IBaseEntity) childBaseValue.getValue()), creditorId);
                    }


                } else {
                    IBaseEntity childBaseEntity = (IBaseEntity) baseValue.getValue();

                    if (childBaseEntity.getValueCount() != 0)
                        prepareClosedDates(childBaseEntity, creditorId);

                }
            }

        }

    }
}
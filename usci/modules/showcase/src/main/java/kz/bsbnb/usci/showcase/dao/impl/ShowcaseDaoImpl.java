package kz.bsbnb.usci.showcase.dao.impl;

import kz.bsbnb.ddlutils.Platform;
import kz.bsbnb.ddlutils.PlatformFactory;
import kz.bsbnb.ddlutils.model.*;
import kz.bsbnb.ddlutils.model.Table;
import kz.bsbnb.usci.core.service.IMetaFactoryService;
import kz.bsbnb.usci.eav.model.base.IBaseEntity;
import kz.bsbnb.usci.eav.model.base.IBaseValue;
import kz.bsbnb.usci.eav.model.base.impl.BaseEntity;
import kz.bsbnb.usci.eav.model.base.impl.BaseSet;
import kz.bsbnb.usci.eav.model.base.impl.OperationType;
import kz.bsbnb.usci.eav.model.meta.IMetaType;
import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;
import kz.bsbnb.usci.eav.model.meta.impl.MetaSet;
import kz.bsbnb.usci.eav.model.meta.impl.MetaValue;
import kz.bsbnb.usci.eav.showcase.ShowCase;
import kz.bsbnb.usci.eav.showcase.ShowCaseField;
import kz.bsbnb.usci.eav.util.DataUtils;
import kz.bsbnb.usci.showcase.ShowcaseHolder;
import kz.bsbnb.usci.showcase.dao.ShowcaseDao;
import org.jooq.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

import static kz.bsbnb.usci.showcase.generated.Tables.*;

@Component
public class ShowcaseDaoImpl implements ShowcaseDao, InitializingBean {
    /* Prefix for showcase table names */
    private final static String TABLES_PREFIX = "R_";

    /* Prefix for showcase column names */
    private final static String COLUMN_PREFIX = "";

    /* Postfix for showcase history tables */
    private final static String HISTORY_POSTFIX = "_HIS";

    /* Same showcases could not be processes in parallel */
    private static final Set<Long> cortegeElements = Collections.synchronizedSet(new HashSet<Long>());

    private final Logger logger = LoggerFactory.getLogger(ShowcaseDaoImpl.class);

    private JdbcTemplate jdbcTemplateSC;

    /* Actual showcase holders */
    private ArrayList<ShowcaseHolder> holders;

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    private DSLContext context;

    @Autowired
    private IMetaFactoryService metaService;

    @Autowired
    public void setDataSourceSC(DataSource dataSourceSC) {
        this.jdbcTemplateSC = new JdbcTemplate(dataSourceSC);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        holders = populateHolders();
    }

    public ArrayList<ShowcaseHolder> getHolders() {
        return holders;
    }

    public ShowcaseHolder getHolderByClassName(String className) {
        for (ShowcaseHolder h : holders) {
            if (h.getShowCaseMeta().getMeta().getClassName().equals(className))
                return h;
        }

        throw new UnknownError("ShowcaseHolder with name: " + className + " not found");
    }

    public void reloadCache() {
        holders = populateHolders();
    }

    private ArrayList<ShowcaseHolder> populateHolders() {
        ArrayList<ShowcaseHolder> holders = new ArrayList<>();
        List<Long> list;

        Select select = context.select(EAV_SC_SHOWCASES.ID).from(EAV_SC_SHOWCASES);
        list = jdbcTemplateSC.queryForList(select.getSQL(), Long.class, select.getBindValues().toArray());

        for (Long id : list) {
            ShowCase showcase = load(id);
            ShowcaseHolder holder = new ShowcaseHolder();
            holder.setShowCaseMeta(showcase);
            holders.add(holder);
        }

        return holders;
    }

    /* Creates both actual & history tables for !isFinal() */
    public void createTables(ShowcaseHolder showcaseHolder) {
        createTable(HistoryState.ACTUAL, showcaseHolder);

        if (!showcaseHolder.getShowCaseMeta().isFinal())
            createTable(HistoryState.HISTORY, showcaseHolder);

        getHolders().add(showcaseHolder);
    }

    /* Creates table for showcase with history state */
    private void createTable(HistoryState historyState, ShowcaseHolder showcaseHolder) {
        String tableName;

        if (historyState == HistoryState.ACTUAL) {
            tableName = getActualTableName(showcaseHolder.getShowCaseMeta());
        } else {
            tableName = getHistoryTableName(showcaseHolder.getShowCaseMeta());
        }

        Database model = new Database();
        model.setName("model");

        Table table = new Table();
        table.setName(tableName);
        //table.setDescription();

        Column idColumn = new Column();
        idColumn.setName("ID");
        idColumn.setPrimaryKey(true);
        idColumn.setRequired(true);
        idColumn.setType("NUMERIC");
        idColumn.setSize("14,0");
        idColumn.setAutoIncrement(true);

        table.addColumn(idColumn);

        Column entityIdColumn = new Column();
        entityIdColumn.setName(showcaseHolder.getRootClassName().toUpperCase() + "_ID");
        entityIdColumn.setPrimaryKey(false);
        entityIdColumn.setRequired(true);
        entityIdColumn.setType("NUMERIC");
        entityIdColumn.setSize("14,0");
        entityIdColumn.setAutoIncrement(false);

        table.addColumn(entityIdColumn);

        for (ShowCaseField field : showcaseHolder.getShowCaseMeta().getFieldsList()) {
            Column column = new Column();
            column.setName(COLUMN_PREFIX + field.getColumnName().toUpperCase());
            column.setPrimaryKey(false);
            column.setRequired(false);

            IMetaType metaType = showcaseHolder.getShowCaseMeta().getActualMeta().getEl(field.getAttributePath());

            if (metaType.isSet() && !metaType.isComplex()) {
                Column simpleArrayColumn = new Column();

                simpleArrayColumn.setName(COLUMN_PREFIX + field.getColumnName().toUpperCase() + "_ID");
                simpleArrayColumn.setPrimaryKey(false);
                simpleArrayColumn.setRequired(false);
                simpleArrayColumn.setType(getDBType(metaType));
                simpleArrayColumn.setSize(getDBSize(metaType));
                simpleArrayColumn.setAutoIncrement(false);

                table.addColumn(simpleArrayColumn);
            }

            column.setType(getDBType(metaType));
            column.setSize(getDBSize(metaType));

            //column.setDefaultValue(xmlReader.getAttributeValue(idx));
            column.setAutoIncrement(false);
            //column.setDescription(xmlReader.getAttributeValue(idx));
            //column.setJavaName(xmlReader.getAttributeValue(idx));

            table.addColumn(column);
        }

        for (ShowCaseField field : showcaseHolder.getShowCaseMeta().getCustomFieldsList()) {
            Column column = new Column();
            column.setName(COLUMN_PREFIX + field.getColumnName().toUpperCase());
            column.setPrimaryKey(false);
            column.setRequired(false);

            IMetaType metaType;

            if (field.getAttributePath().equals("root")) {
                metaType = metaService.getMetaClass(field.getMetaId());
            } else {
                metaType = metaService.getMetaClass(field.getMetaId()).getEl(field.getAttributePath());
            }

            column.setType(getDBType(metaType));
            column.setSize(getDBSize(metaType));

            //column.setDefaultValue(xmlReader.getAttributeValue(idx));
            column.setAutoIncrement(false);
            //column.setDescription(xmlReader.getAttributeValue(idx));
            //column.setJavaName(xmlReader.getAttributeValue(idx));

            table.addColumn(column);
        }

        Column column = new Column();
        column.setName("CDC");
        column.setPrimaryKey(false);
        column.setRequired(false);
        column.setType("DATE");
        table.addColumn(column);

        if (!showcaseHolder.getShowCaseMeta().isFinal()) {
            column = new Column();
            column.setName("OPEN_DATE");
            column.setPrimaryKey(false);
            column.setRequired(false);
            column.setType("DATE");
            table.addColumn(column);

            Index indexOD = new NonUniqueIndex();
            indexOD.setName("ind_" + tableName + "_OPEN_DATE");
            indexOD.addColumn(new IndexColumn("OPEN_DATE"));
            table.addIndex(indexOD);

            column = new Column();
            column.setName("CLOSE_DATE");
            column.setPrimaryKey(false);
            column.setRequired(false);
            column.setType("DATE");
            table.addColumn(column);

            Index indexCD = new NonUniqueIndex();
            indexCD.setName("ind_" + tableName + "_CLOSE_DATE");
            indexCD.addColumn(new IndexColumn("CLOSE_DATE"));
            table.addIndex(indexCD);
        } else {
            column = new Column();
            column.setName("REP_DATE");
            column.setPrimaryKey(false);
            column.setRequired(false);
            column.setType("DATE");
            table.addColumn(column);

            Index indexRD = new NonUniqueIndex();
            indexRD.setName("ind_" + tableName + "_REP_DATE");
            indexRD.addColumn(new IndexColumn("REP_DATE"));
            table.addIndex(indexRD);
        }

        for(Index index:  showcaseHolder.getShowCaseMeta().getIndexes()){
            table.addIndex(index);
        }

        model.addTable(table);

        Platform platform = PlatformFactory.createNewPlatformInstance(jdbcTemplateSC.getDataSource());
        platform.createModel(model, false, true);
    }

    /* Persists generated map to showcase table */
    @Transactional
    private void persistMap(HashMap<ValueElement, Object> map, Date openDate, Date closeDate,
                            ShowcaseHolder showCaseHolder) {
        StringBuilder sql;
        StringBuilder values = new StringBuilder("(");
        String tableName;

        if (closeDate == null)
            tableName = getActualTableName(showCaseHolder.getShowCaseMeta());
        else
            tableName = getHistoryTableName(showCaseHolder.getShowCaseMeta());

        sql = new StringBuilder("INSERT INTO ").append(tableName).append("(");

        Object[] valueArray;

        if (!showCaseHolder.getShowCaseMeta().isFinal()) {
            valueArray = new Object[map.size() + 2];
        } else {
            valueArray = new Object[map.size() + 1];
        }

        int i = 0;

        for (Map.Entry<ValueElement, Object> entry : map.entrySet()) {
            sql.append(COLUMN_PREFIX).append(entry.getKey().columnName).append(", ");
            values.append("?, ");
            valueArray[i++] = entry.getValue();
        }

        if (!showCaseHolder.getShowCaseMeta().isFinal()) {
            sql.append("cdc, open_date, close_date");
            values.append("sysdate, ?, ?)");
            valueArray[i++] = openDate;
            valueArray[i] = closeDate;
        } else {
            sql.append("cdc, rep_date");
            values.append("SYSDATE, ?)");
            valueArray[i] = openDate;
        }

        sql.append(") VALUES ").append(values);

        jdbcTemplateSC.update(sql.toString(), valueArray);
    }

    /* Updates close_date column using @keyData with entity.getReportDate() */
    @Transactional
    private void updateMapLeftRange(HistoryState historyState, KeyData keyData, IBaseEntity entity,
                                    ShowcaseHolder showCaseHolder) {
        String sql = "UPDATE %s SET close_date = ? WHERE " + keyData.queryKeys;

        if (historyState == HistoryState.ACTUAL) {
            sql = String.format(sql, getActualTableName(showCaseHolder.getShowCaseMeta()), COLUMN_PREFIX,
                    showCaseHolder.getRootClassName());
        } else {
            sql = String.format(sql, getHistoryTableName(showCaseHolder.getShowCaseMeta()), COLUMN_PREFIX,
                    showCaseHolder.getRootClassName());
        }

        jdbcTemplateSC.update(sql, getObjectArray(true, keyData.values, entity.getReportDate()));
    }

    private String getActualTableName(ShowCase showCaseMeta) {
        return TABLES_PREFIX + showCaseMeta.getTableName();
    }

    private String getHistoryTableName(ShowCase showCaseMeta) {
        return TABLES_PREFIX + showCaseMeta.getTableName() + HISTORY_POSTFIX;
    }

    /* Does CLOSE operation on showcases */
    @Transactional
    public synchronized void closeEntities(Long scId, IBaseEntity entity, List<ShowcaseHolder> holders) {
        for (ShowcaseHolder holder : holders) {
            if (!holder.getShowCaseMeta().getMeta().getClassName().equals(entity.getMeta().getClassName()))
                continue;

            if (scId == null || scId == 0L || scId == holder.getShowCaseMeta().getId()) {
                if (holder.getShowCaseMeta().getDownPath() == null ||
                        holder.getShowCaseMeta().getDownPath().length() == 0) {
                    closeEntity(entity, holder);
                }
            }
        }
    }

    /* Performs close on entity using holder */
    @Transactional
    private void closeEntity(IBaseEntity entity, ShowcaseHolder holder) {
        String sql;

        sql = "UPDATE %s SET close_date = ? WHERE " + holder.getRootClassName() + "_id = ?";
        sql = String.format(sql, getActualTableName(holder.getShowCaseMeta()),
                COLUMN_PREFIX, holder.getRootClassName());

        jdbcTemplateSC.update(sql, entity.getBaseEntityReportDate().getReportDate(), entity.getId());

        StringBuilder select = new StringBuilder();
        StringBuilder sqlBuilder = new StringBuilder("INSERT INTO %s");

        select.append(COLUMN_PREFIX).append(holder.getRootClassName()).append("_id, ");

        for (ShowCaseField sf : holder.getShowCaseMeta().getFieldsList())
            select.append(COLUMN_PREFIX).append(sf.getColumnName()).append(", ");

        for (ShowCaseField sf : holder.getShowCaseMeta().getCustomFieldsList())
            select.append(COLUMN_PREFIX).append(sf.getColumnName()).append(", ");

        select.append("cdc, open_date, close_date ");
        sqlBuilder.append("(").append(select).append(")( SELECT ")
                .append(select).append("FROM %s WHERE ")
                .append(holder.getRootClassName()).append("_id = ?)");

        String sqlResult = String.format(sqlBuilder.toString(), getHistoryTableName(holder.getShowCaseMeta()),
                getActualTableName(holder.getShowCaseMeta()), COLUMN_PREFIX,
                holder.getRootClassName());

        jdbcTemplateSC.update(sqlResult, entity.getId());

        sql = "DELETE FROM %s WHERE " + holder.getRootClassName() + "_id = ?";
        sql = String.format(sql, getActualTableName(holder.getShowCaseMeta()),
                COLUMN_PREFIX, holder.getRootClassName());

        jdbcTemplateSC.update(sql, entity.getId());
    }

    /* Moves data from actual table to history */
    @Transactional
    private void moveActualMapToHistory(KeyData keyData, ShowcaseHolder showcaseHolder) {
        StringBuilder select = new StringBuilder();
        StringBuilder sql = new StringBuilder("INSERT INTO %s");

        select.append(COLUMN_PREFIX).append(showcaseHolder.getRootClassName()).append("_id, ");

        // default fields
        for (ShowCaseField sf : showcaseHolder.getShowCaseMeta().getFieldsList())
            select.append(COLUMN_PREFIX).append(sf.getColumnName()).append(", ");

        // custom fields
        for (ShowCaseField sf : showcaseHolder.getShowCaseMeta().getCustomFieldsList()) {
            select.append(COLUMN_PREFIX).append(sf.getColumnName()).append(", ");
        }

        select.append("CDC, OPEN_DATE, CLOSE_DATE ");
        sql.append("(").append(select).append(")( SELECT ").append(select)
            .append("FROM %s WHERE ").append(keyData.queryKeys).append(")");

        String sqlResult = String.format(sql.toString(), getHistoryTableName(showcaseHolder.getShowCaseMeta()),
                getActualTableName(showcaseHolder.getShowCaseMeta()), COLUMN_PREFIX,
                showcaseHolder.getRootClassName());

        jdbcTemplateSC.update(sqlResult, keyData.values);

        sqlResult = String.format("DELETE FROM %s WHERE " + keyData.queryKeys + " AND CLOSE_DATE IS NOT NULL",
                getActualTableName(showcaseHolder.getShowCaseMeta()),
                COLUMN_PREFIX, showcaseHolder.getRootClassName());

        jdbcTemplateSC.update(sqlResult, keyData.values);
    }

    /* Physically deletes data from showcase */
    public int deleteById(ShowcaseHolder holder, IBaseEntity e) {
        String sql;
        int rows = 0;

        for (ShowcaseHolder sh : holders) {
            if (!sh.getShowCaseMeta().getTableName().equals(holder.getShowCaseMeta().getTableName()) &&
                    sh.getRootClassName().equals(holder.getRootClassName())) {
                sql = "DELETE FROM %s WHERE %s%s_ID = ?";
                sql = String.format(sql, getHistoryTableName(sh.getShowCaseMeta()),
                        COLUMN_PREFIX, holder.getRootClassName());

                rows += jdbcTemplateSC.update(sql, e.getId());

                sql = "DELETE FROM %s WHERE %s%s_ID = ?";
                sql = String.format(sql, getActualTableName(sh.getShowCaseMeta()),
                        COLUMN_PREFIX, holder.getRootClassName());

                rows += jdbcTemplateSC.update(sql, e.getId());
            }
        }

        sql = "DELETE FROM %s WHERE %s%s_ID = ?";
        sql = String.format(sql, getHistoryTableName(holder.getShowCaseMeta()),
                COLUMN_PREFIX, holder.getRootClassName());

        rows += jdbcTemplateSC.update(sql, e.getId());

        sql = "DELETE FROM %s WHERE %s%s_ID = ?";
        sql = String.format(sql, getActualTableName(holder.getShowCaseMeta()),
                COLUMN_PREFIX, holder.getRootClassName());

        rows += jdbcTemplateSC.update(sql, e.getId());

        return rows;
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public void generate(IBaseEntity globalEntityApplied, ShowcaseHolder showcaseHolder) {
        if (showcaseHolder.getShowCaseMeta().getDownPath() != null) {
            List<BaseEntity> allApplied = (List<BaseEntity>) globalEntityApplied.getEls("{get}" +
                    showcaseHolder.getShowCaseMeta().getDownPath(), true);

            for (BaseEntity baseEntityApplied : allApplied)
                dbCortegeGenerate(globalEntityApplied, baseEntityApplied, showcaseHolder);
        } else {
            dbCortegeGenerate(globalEntityApplied, globalEntityApplied, showcaseHolder);
        }
    }

    /* Performs main operations on showcase  */
    @Transactional
    private void dbCortegeGenerate(IBaseEntity globalEntity, IBaseEntity entity, ShowcaseHolder showcaseHolder) {
        Date openDate, closeDate = null;
        String sql;

        if (globalEntity == null || entity == null || showcaseHolder == null) return;

        HashMap<ArrayElement, HashMap<ValueElement, Object>> savingMap = generateMap(entity, showcaseHolder);

        if (savingMap == null || savingMap.size() == 0)
            return;

        while (true) {
            synchronized (cortegeElements) {
                if (!cortegeElements.contains(showcaseHolder.getShowCaseMeta().getId())) {
                    cortegeElements.add(showcaseHolder.getShowCaseMeta().getId());
                    break;
                }
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }

        try {
            for (Map.Entry<ArrayElement, HashMap<ValueElement, Object>> entry : savingMap.entrySet()) {
                HashMap<ValueElement, Object> entryMap = entry.getValue();

                addCustomKeys(entryMap, globalEntity, showcaseHolder);

                KeyData keyData = new KeyData(entryMap, showcaseHolder.getShowCaseMeta().getKeyFieldsList());

                ValueElement keyValueElement = new ValueElement("_operation", -1L, false);

                if (entryMap.containsKey(keyValueElement)) {
                    OperationType ot = (OperationType) entryMap.get(keyValueElement);
                    switch(ot) {
                        case DELETE:
                            sql = "DELETE FROM %s WHERE " + keyData.queryKeys;
                            sql = String.format(sql, getActualTableName(showcaseHolder.getShowCaseMeta()),
                                    COLUMN_PREFIX, showcaseHolder.getRootClassName());

                            jdbcTemplateSC.update(sql, getObjectArray(false, keyData.values));
                            break;
                        case CLOSE:
                            sql = "UPDATE %s SET close_date = ? WHERE " + keyData.queryKeys;
                            sql = String.format(sql, getActualTableName(showcaseHolder.getShowCaseMeta()),
                                    COLUMN_PREFIX, showcaseHolder.getRootClassName());

                            jdbcTemplateSC.update(sql, getObjectArray(false, getObjectArray(true, keyData.values,
                                    entity.getReportDate())));

                            moveActualMapToHistory(keyData, showcaseHolder);
                            break;
                        default:
                            throw new IllegalStateException("Операция не поддерживается(" + ot + ")!;");
                    }
                    continue;
                }

                if (!showcaseHolder.getShowCaseMeta().isFinal()) {
                    try {
                        sql = "SELECT MAX(open_date) AS open_date FROM %s WHERE " + keyData.queryKeys;
                        sql = String.format(sql, getActualTableName(showcaseHolder.getShowCaseMeta()),
                                COLUMN_PREFIX, showcaseHolder.getRootClassName().toUpperCase());

                        openDate = (Date) jdbcTemplateSC.queryForMap(sql, keyData.values).get("OPEN_DATE");
                    } catch (EmptyResultDataAccessException e) {
                        openDate = null;
                    }

                    boolean compResult;

                    if (openDate == null) {
                        openDate = entity.getReportDate();
                    } else if (openDate.compareTo(entity.getReportDate()) == 0) {
                        openDate = entity.getReportDate();

                        sql = "DELETE FROM %s WHERE " + keyData.queryKeys + " and open_date = ?";
                        sql = String.format(sql, getActualTableName(showcaseHolder.getShowCaseMeta()),
                                COLUMN_PREFIX, showcaseHolder.getRootClassName());

                        jdbcTemplateSC.update(sql, getObjectArray(false, keyData.values, openDate));
                    } else if (openDate.compareTo(entity.getReportDate()) < 0) {
                        compResult = compareValues(HistoryState.ACTUAL, entryMap, entity, showcaseHolder, keyData);

                        if (compResult) continue;

                        updateMapLeftRange(HistoryState.ACTUAL, keyData, entity, showcaseHolder);
                        moveActualMapToHistory(keyData, showcaseHolder);

                        openDate = entity.getReportDate();
                    } else {
                        sql = "SELECT MIN(open_date) as open_date FROM %s WHERE " + keyData.queryKeys +
                                " AND open_date > ? ";

                        sql = String.format(sql, getHistoryTableName(showcaseHolder.getShowCaseMeta()),
                                COLUMN_PREFIX, showcaseHolder.getRootClassName());

                        closeDate = (Date) jdbcTemplateSC.queryForMap(sql,
                                getObjectArray(false, keyData.values, entity.getReportDate())).get("OPEN_DATE");

                        if (closeDate == null) {
                            compResult = compareValues(HistoryState.ACTUAL, entryMap, entity, showcaseHolder, keyData);

                            if (compResult) {
                                sql = "UPDATE %s SET open_date = ? WHERE " + keyData.queryKeys + " AND open_date = ?";
                                sql = String.format(sql, getActualTableName(showcaseHolder.getShowCaseMeta()),
                                        COLUMN_PREFIX, showcaseHolder.getRootClassName());

                                jdbcTemplateSC.update(sql, getObjectArray(false, getObjectArray(true, keyData.values,
                                        entity.getReportDate()), openDate));

                                continue;
                            } else {
                                closeDate = openDate;
                            }
                        } else {
                            compResult = compareValues(HistoryState.HISTORY, entryMap, entity, showcaseHolder, keyData);

                            if (compResult) {
                                sql = "UPDATE %s SET open_date = ? WHERE " + keyData.queryKeys + " AND open_date = ?";
                                sql = String.format(sql, getHistoryTableName(showcaseHolder.getShowCaseMeta()),
                                        COLUMN_PREFIX, showcaseHolder.getRootClassName());

                                jdbcTemplateSC.update(sql, getObjectArray(false, getObjectArray(true, keyData.values,
                                        entity.getReportDate()), closeDate));

                                continue;
                            } else {
                                closeDate = openDate;
                            }
                        }

                        openDate = entity.getReportDate();
                        updateMapLeftRange(HistoryState.HISTORY, keyData, entity, showcaseHolder);
                    }
                } else {
                    openDate = entity.getReportDate();

                    sql = "DELETE FROM %s WHERE " + keyData.queryKeys + " and rep_date = ?";
                    sql = String.format(sql, getActualTableName(showcaseHolder.getShowCaseMeta()),
                            COLUMN_PREFIX, showcaseHolder.getRootClassName());

                    jdbcTemplateSC.update(sql, getObjectArray(false, keyData.values, openDate));
                }

                persistMap(entryMap, openDate, closeDate, showcaseHolder);
            }
        } finally {
            synchronized (cortegeElements) {
                cortegeElements.remove(showcaseHolder.getShowCaseMeta().getId());
            }
        }
    }

    /* Adds custom keys to existing map */
    public void addCustomKeys(HashMap<ValueElement, Object> entryMap, IBaseEntity globalEntity,
                              ShowcaseHolder showcaseHolder) {
        for (ShowCaseField sf : showcaseHolder.getShowCaseMeta().getCustomFieldsList()) {
            if (sf.getAttributePath().equals("root")) {
                entryMap.put(new ValueElement(sf.getColumnName(), globalEntity.getId()), globalEntity.getId());
                continue;
            }

            Object customObject = null;

            try {
                customObject = globalEntity.getEl(sf.getAttributePath());
            } catch(Exception e) {
                e.printStackTrace();
            }

            try {
                if (customObject instanceof BaseEntity) {
                    entryMap.put(new ValueElement(sf.getColumnName(), ((BaseEntity) customObject).getId()),
                            ((BaseEntity) customObject).getId());
                } else if (customObject instanceof BaseSet) {
                    throw new UnsupportedOperationException("CustomSet is not supported!");
                } else {
                    entryMap.put(new ValueElement(sf.getColumnName(), 0L), customObject);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* Returns array elementArray + elements in  both order*/
    private Object[] getObjectArray(boolean reverse, Object[] elementArray, Object... elements) {
        Object[] newObjectArray = new Object[elementArray.length + elements.length];

        int index = 0;
        if (!reverse) {
            for (Object object : elementArray) newObjectArray[index++] = object;
            for (Object object : elements) newObjectArray[index++] = object;
        } else {
            for (Object object : elements) newObjectArray[index++] = object;
            for (Object object : elementArray) newObjectArray[index++] = object;
        }

        return newObjectArray;
    }

    /* Generates path for relational tables using showcaseHolder */
    private HashMap<String, HashSet<PathElement>> generatePaths(IBaseEntity entity, ShowcaseHolder showcaseHolder,
                                                                HashSet<PathElement> keyPaths) {
        HashMap<String, HashSet<PathElement>> paths = new HashMap<>();

        HashSet<PathElement> tmpSet;

        for (ShowCaseField sf : showcaseHolder.getShowCaseMeta().getFieldsList()) {
            IMetaType attributeMetaType = entity.getMeta().getEl(sf.getAttributePath());

            if (sf.getAttributePath().contains(".")) {
                if (attributeMetaType.isComplex()) {
                    if (paths.get("root." + sf.getAttributePath()) != null) {
                        tmpSet = paths.get("root." + sf.getAttributePath());
                    } else {
                        tmpSet = new HashSet<>();
                    }

                    tmpSet.add(new PathElement("root", sf.getAttributePath(), sf.getColumnName()));
                    paths.put("root." + sf.getAttributePath(), tmpSet);

                    String path = sf.getAttributePath().substring(0, sf.getAttributePath().lastIndexOf("."));
                    String name = sf.getAttributePath().substring(sf.getAttributePath().lastIndexOf(".") + 1);

                    if (paths.get("root." + path) != null) {
                        tmpSet = paths.get("root." + path);
                    } else {
                        tmpSet = new HashSet<>();
                    }

                    tmpSet.add(new PathElement(name, sf.getAttributePath(), sf.getColumnName()));
                    paths.put("root." + path, tmpSet);
                } else {
                    String path = sf.getAttributePath().substring(0, sf.getAttributePath().lastIndexOf("."));
                    String name = sf.getAttributePath().substring(sf.getAttributePath().lastIndexOf(".") + 1);

                    if (paths.get("root." + path) != null) {
                        tmpSet = paths.get("root." + path);
                    } else {
                        tmpSet = new HashSet<>();
                    }

                    tmpSet.add(new PathElement(name, sf.getAttributePath(), sf.getColumnName()));
                    paths.put("root." + path, tmpSet);
                }
            } else {
                if (paths.get("root") != null) {
                    tmpSet = paths.get("root");
                } else {
                    tmpSet = new HashSet<>();
                }

                if (attributeMetaType.isSet()) {
                    keyPaths.add(new PathElement("root." + sf.getAttributePath(), sf.getAttributePath(),
                            sf.getColumnName()));

                    tmpSet.add(new PathElement("root." + sf.getAttributePath(), sf.getAttributePath(),
                            sf.getColumnName()));
                    paths.put("root", tmpSet);

                    tmpSet = new HashSet<>();
                    tmpSet.add(new PathElement("root", sf.getAttributePath(), sf.getColumnName()));
                    paths.put("root." + sf.getAttributePath(), tmpSet);
                } else if (attributeMetaType.isComplex()) {
                    tmpSet.add(new PathElement("root." + sf.getAttributePath(), sf.getAttributePath(),
                            sf.getColumnName()));
                    paths.put("root", tmpSet);

                    tmpSet = new HashSet<>();
                    tmpSet.add(new PathElement("root", sf.getAttributePath(), sf.getColumnName()));
                    paths.put("root." + sf.getAttributePath(), tmpSet);
                } else {
                    tmpSet.add(new PathElement(sf.getAttributePath(), sf.getAttributePath(), sf.getColumnName()));

                    paths.put("root", tmpSet);
                }
            }
        }

        return paths;
    }

    private HashMap<ValueElement, Object> readMap(String curPath, IBaseEntity entity, HashMap<String,
            HashSet<PathElement>> paths, boolean parentIsArray) {
        HashSet<PathElement> attributes = paths.get(curPath);

        HashMap<ValueElement, Object> map = new HashMap<>();

        if (entity.getOperation() != null)
            map.put(new ValueElement("_operation", -1L, false), entity.getOperation());

        if (attributes != null) {
            for (PathElement attribute : attributes) {
                if (attribute.elementPath.equals("root")) {
                    map.put(new ValueElement(attribute.columnName, entity.getId()), entity.getId());
                } else {
                    if (attribute.elementPath.contains("root.")) {
                        Object container = entity.getEl(attribute.elementPath.substring(
                                attribute.elementPath.indexOf(".") + 1));

                        if (container == null) continue;

                        if (container instanceof BaseEntity) {
                            BaseEntity innerEntity = (BaseEntity) container;

                            map.put(new ValueElement(attribute.columnName, innerEntity.getId()),
                                    readMap(attribute.elementPath, innerEntity, paths, false));
                        } else if (container instanceof BaseSet) {
                            BaseSet innerSet = (BaseSet) container;

                            HashMap<ValueElement, Object> arrayMap = new HashMap<>();

                            if (innerSet.getMemberType().isComplex()) {
                                for (IBaseValue bValue : innerSet.get()) {
                                    BaseEntity bValueEntity = (BaseEntity) bValue.getValue();
                                    arrayMap.put(new ValueElement(attribute.elementPath, bValueEntity.getId(), false),
                                            readMap(attribute.elementPath, bValueEntity, paths, true));
                                }

                                map.put(new ValueElement(attribute.elementPath, ((BaseSet) container).getId(),
                                        true, false), arrayMap);
                            } else {
                                for (IBaseValue bValue : innerSet.get())
                                    arrayMap.put(new ValueElement(attribute.elementPath, bValue.getId(), false),
                                            bValue.getValue());

                                map.put(new ValueElement(attribute.elementPath, ((BaseSet) container).getId(),
                                        true, true), arrayMap);
                            }
                        }
                    } else {
                        IBaseValue iBaseValue = entity.getBaseValue(attribute.elementPath);

                        if (iBaseValue != null && iBaseValue.getMetaAttribute().getMetaType().isComplex() &&
                                !iBaseValue.getMetaAttribute().getMetaType().isSet()) {
                            map.put(new ValueElement(attribute.columnName, iBaseValue.getId()), readMap(curPath + "."
                                    + attribute.elementPath, (BaseEntity) iBaseValue.getValue(), paths, false));
                        } else if (iBaseValue != null && iBaseValue.getMetaAttribute().getMetaType().isComplex() &&
                                iBaseValue.getMetaAttribute().getMetaType().isSet()) {
                            throw new UnsupportedOperationException("Complex entity cannot contain complex set");
                        } else if (iBaseValue != null && iBaseValue.getValue() instanceof BaseSet) {
                            BaseSet bSet = (BaseSet) iBaseValue.getValue();

                            HashMap<ValueElement, Object> arrayMap = new HashMap<>();

                            for (IBaseValue innerValue : bSet.get())
                                arrayMap.put(new ValueElement(attribute.elementPath, innerValue.getId(), false, true),
                                        innerValue.getValue());

                            map.put(new ValueElement(attribute.elementPath, iBaseValue.getId(), true, true),
                                    arrayMap);
                        } else if (iBaseValue != null) {
                            map.put(new ValueElement(attribute.columnName, iBaseValue.getId()), iBaseValue.getValue());
                        }
                    }
                }
            }
        }

        return map;
    }

    @SuppressWarnings("unchecked")
    private HashMap<ArrayElement, HashMap<ValueElement, Object>> gen(HashMap<ValueElement, Object> dirtyMap) {
        HashMap<ArrayElement, HashMap<ValueElement, Object>> arrayEl = new HashMap<>();

        int index = 0;

        for (Map.Entry<ValueElement, Object> entry : dirtyMap.entrySet()) {
            if (entry.getKey().isArray && !entry.getKey().isSimple) {
                HashMap<ValueElement, Object> innerMap = (HashMap<ValueElement, Object>) entry.getValue();

                for (Map.Entry<ValueElement, Object> innerEntry : innerMap.entrySet()) {
                    HashMap<ArrayElement, HashMap<ValueElement, Object>> recursiveMap =
                            gen((HashMap) innerEntry.getValue());

                    for (Map.Entry<ArrayElement, HashMap<ValueElement, Object>> recEntry : recursiveMap.entrySet()) {
                        arrayEl.put(new ArrayElement(index++, innerEntry.getKey()), (HashMap) recEntry.getValue());
                    }
                }
            } else if (entry.getKey().isArray && entry.getKey().isSimple) {
                HashMap<ValueElement, Object> innerMap = (HashMap) entry.getValue();

                for (Map.Entry<ValueElement, Object> innerEntry : innerMap.entrySet()) {
                    HashMap<ValueElement, Object> newHashMap = new HashMap<>();
                    newHashMap.put(innerEntry.getKey(), innerEntry.getValue());

                    newHashMap.put(new ValueElement(innerEntry.getKey().columnName + "_id",
                            innerEntry.getKey().elementId), innerEntry.getKey().elementId);

                    arrayEl.put(new ArrayElement(index++, innerEntry.getKey()), newHashMap);
                }
            }
        }

        if (arrayEl.size() > 0) {
            for (Map.Entry<ValueElement, Object> entry : dirtyMap.entrySet()) {
                if (!entry.getKey().isArray) {
                    for (ArrayElement element : arrayEl.keySet()) {
                        HashMap<ValueElement, Object> tempMap = arrayEl.get(element);
                        tempMap.put(entry.getKey(), entry.getValue());
                        arrayEl.put(element, tempMap);
                    }
                }
            }
        } else {
            HashMap<ValueElement, Object> singleMap = new HashMap<>();

            for (Map.Entry<ValueElement, Object> entry : dirtyMap.entrySet()) {
                singleMap.put(entry.getKey(), entry.getValue());
            }

            arrayEl.put(new ArrayElement(index, new ValueElement("root", 0L)), singleMap);
        }

        return arrayEl;
    }

    @SuppressWarnings("unchecked")
    private HashMap<ValueElement, Object> clearDirtyMap(HashMap<ValueElement, Object> dirtyMap) {
        HashMap<ValueElement, Object> returnMap = new HashMap<>();

        for (Map.Entry<ValueElement, Object> entry : dirtyMap.entrySet()) {
            if (entry.getValue() instanceof HashMap) {
                HashMap<ValueElement, Object> tmpMap = clearDirtyMap((HashMap) entry.getValue());

                for (Map.Entry<ValueElement, Object> tmpMapEntry : tmpMap.entrySet())
                    returnMap.put(tmpMapEntry.getKey(), tmpMapEntry.getValue());
            } else {
                returnMap.put(entry.getKey(), entry.getValue());
            }
        }

        return returnMap;
    }

    private HashMap<ArrayElement, HashMap<ValueElement, Object>> generateMap(IBaseEntity entity,
                                                                             ShowcaseHolder showcaseHolder) {
        HashSet<PathElement> keyPaths = new HashSet<>();
        HashMap<String, HashSet<PathElement>> paths = generatePaths(entity, showcaseHolder, keyPaths);

        HashSet<PathElement> rootAttributes;

        if (paths.size() == 0 || paths.get("root") == null) {
            rootAttributes = new HashSet<>();
        } else {
            rootAttributes = paths.get("root");
        }

        rootAttributes.add(new PathElement("root", "root", entity.getMeta().getClassName() + "_id"));
        keyPaths.add(new PathElement("root", "root", entity.getMeta().getClassName() + "_id"));

        HashMap<ValueElement, Object> dirtyMap = readMap("root", entity, paths, false);

        if (dirtyMap == null)
            return null;

        HashMap<ArrayElement, HashMap<ValueElement, Object>> globalMap = gen(dirtyMap);
        HashMap<ArrayElement, HashMap<ValueElement, Object>> clearedGlobalMap = new HashMap<>();

        for (Map.Entry<ArrayElement, HashMap<ValueElement, Object>> globalEntry : globalMap.entrySet()) {
            HashMap<ValueElement, Object> tmpMap = clearDirtyMap(globalEntry.getValue());
            boolean hasMandatoryKeys = true;

            for (PathElement pElement : keyPaths) {
                boolean eFound = false;
                for (ValueElement vElement : tmpMap.keySet()) {
                    if (vElement.columnName.equals(pElement.columnName)) {
                        eFound = true;
                        break;
                    }
                }

                if (!eFound) {
                    hasMandatoryKeys = false;
                    break;
                }
            }

            if (hasMandatoryKeys)
                clearedGlobalMap.put(globalEntry.getKey(), tmpMap);
        }

        return clearedGlobalMap;
    }

    private boolean compareValues(HistoryState state, HashMap<ValueElement, Object> savingMap,
                                  IBaseEntity entity, ShowcaseHolder showcaseHolder, KeyData keyData) {
        StringBuilder st = new StringBuilder();
        boolean equalityFlag = true;

        try {
            int colCounter = 0;
            for (ValueElement valueElement : savingMap.keySet()) {
                st.append(valueElement.columnName);

                if (++colCounter < savingMap.size())
                    st.append(", ");
            }

            String sql = "SELECT " + st.toString() + " FROM %s WHERE " + keyData.queryKeys;

            Map dbElement;

            if (state == HistoryState.ACTUAL) {
                sql = String.format(sql, getActualTableName(showcaseHolder.getShowCaseMeta()),
                        COLUMN_PREFIX, showcaseHolder.getRootClassName());

                dbElement = jdbcTemplateSC.queryForMap(sql, keyData.values);
            } else {
                sql += " AND open_date = ?";
                sql = String.format(sql, getHistoryTableName(showcaseHolder.getShowCaseMeta()),
                        COLUMN_PREFIX, showcaseHolder.getRootClassName(), entity.getReportDate());

                dbElement = jdbcTemplateSC.queryForMap(sql, getObjectArray(false, keyData.values,
                        entity.getReportDate()));
            }

            for (ValueElement valueElement : savingMap.keySet()) {
                Object newValue = savingMap.get(valueElement);
                Object dbValue = dbElement.get(valueElement.columnName);

                if (newValue == null && dbValue == null)
                    continue;

                if (newValue == null || dbValue == null) {
                    equalityFlag = false;
                    break;
                }

                if (newValue instanceof Double) {
                    double value = ((BigDecimal) dbValue).doubleValue();
                    if (!newValue.equals(value)) {
                        equalityFlag = false;
                        break;
                    }
                } else if (newValue instanceof Integer) {
                    int value = ((BigDecimal) dbValue).intValue();
                    if (!newValue.equals(value)) {
                        equalityFlag = false;
                        break;
                    }
                } else if (newValue instanceof Boolean) {
                    boolean value = ((BigDecimal) dbValue).longValue() == 1;
                    if (!newValue.equals(value)) {
                        equalityFlag = false;
                        break;
                    }
                } else if (newValue instanceof Long) {
                    if (!newValue.equals(Long.valueOf(dbValue.toString()))) {
                        equalityFlag = false;
                        break;
                    }
                } else if (newValue instanceof String) {
                    if (!newValue.toString().equals(dbValue.toString())) {
                        equalityFlag = false;
                        break;
                    }
                } else if (newValue instanceof Date) {
                    Date value = DataUtils.convertToSQLDate((Timestamp) dbValue);
                    if (!newValue.equals(value)) {
                        equalityFlag = false;
                        break;
                    }
                } else {
                    if (!newValue.equals(dbValue)) {
                        equalityFlag = false;
                        break;
                    }
                }
            }

            return equalityFlag;
        } catch (IncorrectResultSizeDataAccessException ir) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String getDBType(IMetaType type) {
        if (type.isComplex())
            return "NUMERIC";

        if (type instanceof MetaSet)
            type = ((MetaSet) type).getMemberType();

        if (type.isSet())
            throw new IllegalArgumentException("ShowCase can't contain set columns: " +
                    type.toString());

        MetaValue metaValue = (MetaValue) type;

        switch (metaValue.getTypeCode()) {
            case INTEGER:
                return "NUMERIC";
            case DATE:
                return "DATE";
            case STRING:
                return "VARCHAR";
            case BOOLEAN:
                return "NUMERIC";
            case DOUBLE:
                return "NUMERIC";
            default:
                throw new IllegalArgumentException("Unknown simple type code");
        }
    }

    private String getDBSize(IMetaType type) {
        if (type.isComplex())
            return "14, 0";

        if (type instanceof MetaSet)
            type = ((MetaSet) type).getMemberType();

        if (type.isSet())
            throw new IllegalArgumentException("ShowCase can't contain set columns");

        MetaValue metaValue = (MetaValue) type;

        switch (metaValue.getTypeCode()) {
            case INTEGER:
                return "14,0";
            case DATE:
                return null;
            case STRING:
                return "1024";
            case BOOLEAN:
                return "1";
            case DOUBLE:
                return "17,3";
            default:
                throw new IllegalArgumentException("Unknown simple type code");
        }
    }

    @Override
    public ShowCase load(long id) {
        Select select = context
                .select(EAV_SC_SHOWCASES.ID,
                        EAV_SC_SHOWCASES.TITLE,
                        EAV_SC_SHOWCASES.TABLE_NAME,
                        EAV_SC_SHOWCASES.NAME,
                        EAV_SC_SHOWCASES.CLASS_NAME,
                        EAV_SC_SHOWCASES.DOWN_PATH,
                        EAV_SC_SHOWCASES.IS_FINAL)
                .from(EAV_SC_SHOWCASES)
                .where(EAV_SC_SHOWCASES.ID.equal(id));

        logger.debug(select.toString());

        List<Map<String, Object>> rows = jdbcTemplateSC.queryForList(select.getSQL(),
                select.getBindValues().toArray());

        if (rows.size() > 1)
            throw new RuntimeException("Query for ShowCase return more than one row.");

        if (rows.size() < 1)
            throw new RuntimeException("ShowCase not found.");

        Map<String, Object> row = rows.iterator().next();

        ShowCase showCase = new ShowCase();
        showCase.setId(id);
        showCase.setName((String) row.get(EAV_SC_SHOWCASES.NAME.getName()));
        showCase.setTableName((String) row.get(EAV_SC_SHOWCASES.TABLE_NAME.getName()));
        showCase.setDownPath((String) row.get(EAV_SC_SHOWCASES.DOWN_PATH.getName()));
        showCase.setFinal((row.get(EAV_SC_SHOWCASES.IS_FINAL.getName())).toString().equals("1"));

        String metaClassName = (String) row.get(EAV_SC_SHOWCASES.CLASS_NAME.getName());
        MetaClass metaClass = metaService.getMetaClass(metaClassName);
        showCase.setMeta(metaClass);

        select = context
                .select(EAV_SC_SHOWCASE_FIELDS.ID,
                        EAV_SC_SHOWCASE_FIELDS.COLUMN_NAME,
                        EAV_SC_SHOWCASE_FIELDS.ATTRIBUTE_ID,
                        EAV_SC_SHOWCASE_FIELDS.ATTRIBUTE_PATH,
                        EAV_SC_SHOWCASE_FIELDS.TYPE)
                .from(EAV_SC_SHOWCASE_FIELDS)
                .where(EAV_SC_SHOWCASE_FIELDS.SHOWCASE_ID.equal(showCase.getId()));

        logger.debug(select.toString());

        rows = jdbcTemplateSC.queryForList(select.getSQL(), select.getBindValues().toArray());

        if (rows.size() > 0) {
            for (Map<String, Object> curRow : rows) {
                ShowCaseField showCaseField = new ShowCaseField();
                showCaseField.setColumnName((String) curRow.get(EAV_SC_SHOWCASE_FIELDS.COLUMN_NAME.getName()));
                showCaseField.setType(((BigDecimal) curRow.get(EAV_SC_SHOWCASE_FIELDS.TYPE.getName())).intValue());
                showCaseField.setAttributePath((String) curRow.
                        get(EAV_SC_SHOWCASE_FIELDS.ATTRIBUTE_PATH.getName()));

                showCaseField.setAttributeId(((BigDecimal) curRow
                        .get(EAV_SC_SHOWCASE_FIELDS.ATTRIBUTE_ID.getName())).longValue());

                showCaseField.setId(((BigDecimal) curRow
                        .get(EAV_SC_SHOWCASE_FIELDS.ID.getName())).longValue());

                if (showCaseField.getType() == ShowCaseField.ShowCaseFieldTypes.CUSTOM) {
                    showCase.addCustomField(showCaseField);
                } else if (showCaseField.getType() == ShowCaseField.ShowCaseFieldTypes.KEY) {
                    showCase.addKeyField(showCaseField);
                } else {
                    showCase.addField(showCaseField);
                }
            }
        }

        return showCase;
    }

    @Override
    public ShowCase load(String name) {
        Long id = getIdByName(name);
        return load(id);
    }

    private long getIdByName(String name) {
        Select select = context.select(EAV_SC_SHOWCASES.ID).from(EAV_SC_SHOWCASES)
                .where(EAV_SC_SHOWCASES.NAME.equal(name));

        logger.debug(select.toString());

        List<Map<String, Object>> rows = jdbcTemplateSC.queryForList(select.getSQL(),
                select.getBindValues().toArray());

        if (rows.size() > 1)
            throw new RuntimeException("Query for ShowCase return more than one row.");

        if (rows.size() < 1)
            return 0;

        Map<String, Object> row = rows.iterator().next();

        return ((BigDecimal) row
                .get(EAV_SC_SHOWCASES.ID.getName())).longValue();
    }

    @Override
    @Transactional
    public long save(ShowCase showCaseForSave) {
        if (showCaseForSave.getId() < 1)
            showCaseForSave.setId(getIdByName(showCaseForSave.getName()));

        if (showCaseForSave.getId() < 1) {
            return insert(showCaseForSave);
        } else {
            update(showCaseForSave);
            return showCaseForSave.getId();
        }
    }

    @Override
    public void remove(ShowCase showCase) {
        throw new RuntimeException("Unimplemented");
    }

    private long insertField(ShowCaseField showCaseField, long showCaseId) {
        Insert insert = context
                .insertInto(EAV_SC_SHOWCASE_FIELDS)
                .set(EAV_SC_SHOWCASE_FIELDS.META_ID, showCaseField.getMetaId())
                .set(EAV_SC_SHOWCASE_FIELDS.ATTRIBUTE_ID, showCaseField.getAttributeId())
                .set(EAV_SC_SHOWCASE_FIELDS.ATTRIBUTE_PATH, showCaseField.getAttributePath())
                .set(EAV_SC_SHOWCASE_FIELDS.COLUMN_NAME, showCaseField.getColumnName())
                .set(EAV_SC_SHOWCASE_FIELDS.SHOWCASE_ID, showCaseId)
                .set(EAV_SC_SHOWCASE_FIELDS.TYPE, showCaseField.getType());

        logger.debug(insert.toString());

        long showCaseFieldId = insertWithId(insert.getSQL(),
                insert.getBindValues().toArray());

        showCaseField.setId(showCaseFieldId);

        return showCaseFieldId;
    }

    private long insert(ShowCase showCase) {
        Insert insert = context
                .insertInto(EAV_SC_SHOWCASES)
                .set(EAV_SC_SHOWCASES.NAME, showCase.getName())
                .set(EAV_SC_SHOWCASES.TABLE_NAME, showCase.getTableName())
                .set(EAV_SC_SHOWCASES.CLASS_NAME, showCase.getMeta().getClassName())
                .set(EAV_SC_SHOWCASES.DOWN_PATH, showCase.getDownPath())
                .set(EAV_SC_SHOWCASES.IS_FINAL, showCase.isFinal() ? 1 : 0);

        logger.debug(insert.toString());

        long showCaseId = insertWithId(insert.getSQL(),
                insert.getBindValues().toArray());

        showCase.setId(showCaseId);

        for (ShowCaseField sf : showCase.getFieldsList())
            insertField(sf, showCaseId);

        for (ShowCaseField sf : showCase.getCustomFieldsList())
            insertField(sf, showCaseId);

        for (ShowCaseField sf : showCase.getKeyFieldsList())
            insertField(sf, showCaseId);

        return showCaseId;
    }

    private long deleteFields(long showCaseId) {
        Delete delete = context
                .delete(EAV_SC_SHOWCASE_FIELDS)
                .where(EAV_SC_SHOWCASE_FIELDS.SHOWCASE_ID.equal(showCaseId));

        logger.debug(delete.toString());
        return jdbcTemplateSC.update(delete.getSQL(), delete.getBindValues().toArray());
    }

    private void update(ShowCase showCaseSaving) {
        if (showCaseSaving.getId() < 1)
            throw new IllegalArgumentException("UPDATE couldn't be done without ID.");

        String tableAlias = "sc";
        Update update = context
                .update(EAV_SC_SHOWCASES.as(tableAlias))
                .set(EAV_SC_SHOWCASES.as(tableAlias).NAME, showCaseSaving.getName())
                .set(EAV_SC_SHOWCASES.as(tableAlias).TABLE_NAME, showCaseSaving.getTableName())
                .set(EAV_SC_SHOWCASES.as(tableAlias).DOWN_PATH, showCaseSaving.getDownPath())
                .set(EAV_SC_SHOWCASES.as(tableAlias).IS_FINAL, showCaseSaving.isFinal() ? 1 : 0)
                .where(EAV_SC_SHOWCASES.as(tableAlias).as(tableAlias).ID.equal(showCaseSaving.getId()));

        logger.debug(update.toString());
        int count = jdbcTemplateSC.update(update.getSQL(), update.getBindValues().toArray());

        if (count != 1)
            throw new RuntimeException("UPDATE operation should be update only one record.");

        deleteFields(showCaseSaving.getId());

        for (ShowCaseField sf : showCaseSaving.getFieldsList())
            insertField(sf, showCaseSaving.getId());

        for (ShowCaseField sf : showCaseSaving.getCustomFieldsList())
            insertField(sf, showCaseSaving.getId());
    }

    private long insertWithId(String query, Object[] values) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplateSC.update(new GenericInsertPreparedStatementCreator(query, values), keyHolder);
        return keyHolder.getKey().longValue();
    }

    @Override
    public Long insertBadEntity(Long entityId, Long scId, Date report_date, String stackTrace, String message) {
        if (scId == null)
            scId = 0L;

        Insert insert = context
                .insertInto(EAV_SC_BAD_ENTITIES)
                .set(EAV_SC_BAD_ENTITIES.ENTITY_ID, entityId)
                .set(EAV_SC_BAD_ENTITIES.SC_ID, scId)
                .set(EAV_SC_BAD_ENTITIES.REPORT_DATE, DataUtils.convert(report_date))
                .set(EAV_SC_BAD_ENTITIES.STACK_TRACE, stackTrace)
                .set(EAV_SC_BAD_ENTITIES.MESSAGE, message);

        if (logger.isDebugEnabled())
            logger.debug(insert.toString());

        return insertWithId(insert.getSQL(), insert.getBindValues().toArray());
    }

    public enum HistoryState {
        ACTUAL,
        HISTORY
    }

    class KeyData {
        final Object[] keys;
        final Object[] values;
        String queryKeys = "";

        public KeyData(HashMap<ValueElement, Object> map, List<ShowCaseField> keyFields) {
            keys = new Object[keyFields.size()];
            values = new Object[keyFields.size()];

            int i = 0;
            for (ShowCaseField sf : keyFields) {
                keys[i] = sf.getColumnName();

                for (Map.Entry<ValueElement, Object> entry : map.entrySet()) {
                    if (entry.getKey().columnName.equals(sf.getColumnName())) {
                        values[i] = entry.getValue();
                        break;
                    }
                }

                queryKeys += sf.getColumnName() + " = ? ";
                if (++i < keyFields.size()) queryKeys += " AND ";
            }
        }
    }

    class PathElement {
        public final String elementPath;
        public final String attributePath;
        public final String columnName;

        public PathElement(String elementPath, String attributePath, String columnName) {
            this.elementPath = elementPath;
            this.attributePath = attributePath;
            this.columnName = columnName;
        }

        @Override
        public String toString() {
            return "PathElement{" +
                    "elementPath='" + elementPath + '\'' +
                    ", attributePath='" + attributePath + '\'' +
                    ", columnName='" + columnName + '\'' +
                    '}';
        }
    }

    class ValueElement {
        public final String columnName;
        public final Long elementId;
        public final boolean isArray;
        public final boolean isSimple;

        public ValueElement(String columnName, Long elementId) {
            this.columnName = columnName;
            this.elementId = elementId;
            this.isArray = false;
            this.isSimple = false;
        }

        public ValueElement(String columnName, Long elementId, boolean isArray) {
            this.columnName = columnName;
            this.elementId = elementId;
            this.isArray = isArray;
            this.isSimple = false;
        }

        public ValueElement(String columnName, Long elementId, boolean isArray, boolean isSimple) {
            this.columnName = columnName;
            this.elementId = elementId;
            this.isArray = isArray;
            this.isSimple = isSimple;
        }

        @Override
        public String toString() {
            return "ValueElement{" +
                    "columnName='" + columnName + '\'' +
                    ", elementId=" + elementId +
                    ", isArray=" + isArray +
                    ", isSimple=" + isSimple +
                    '}';
        }

        @SuppressWarnings("all")
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ValueElement that = (ValueElement) o;

            if (isArray != that.isArray) return false;
            if (isSimple != that.isSimple) return false;
            if (!columnName.equals(that.columnName)) return false;
            return elementId.equals(that.elementId);

        }

        @Override
        public int hashCode() {
            int result = columnName.hashCode();
            result = 31 * result + elementId.hashCode();
            result = 31 * result + (isArray ? 1 : 0);
            result = 31 * result + (isSimple ? 1 : 0);
            return result;
        }
    }

    class ArrayElement {
        public final int index;
        public final ValueElement valueElement;

        public ArrayElement(int index, ValueElement valueElement) {
            this.index = index;
            this.valueElement = valueElement;
        }

        @Override
        public String toString() {
            return "ArrayElement{" +
                    "index=" + index +
                    ", valueElement=" + valueElement +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ArrayElement that = (ArrayElement) o;

            return index == that.index && valueElement.equals(that.valueElement);

        }

        @Override
        public int hashCode() {
            int result = valueElement.hashCode();
            result = 31 * result + index;
            return result;
        }
    }

    class GenericInsertPreparedStatementCreator implements PreparedStatementCreator {
        final String query;
        final Object[] values;
        final String keyName = "id";

        public GenericInsertPreparedStatementCreator(String query, Object[] values) {
            this.query = query;
            this.values = values.clone();
        }

        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            PreparedStatement ps = con.prepareStatement(
                    query, new String[]{keyName});

            int i = 1;
            for (Object obj : values) {
                ps.setObject(i++, obj);
            }

            return ps;
        }
    }
}

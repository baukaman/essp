package kz.bsbnb.usci.eav.persistance.searcher.impl;

import kz.bsbnb.usci.eav.model.base.IBaseEntity;
import kz.bsbnb.usci.eav.model.base.IBaseValue;
import kz.bsbnb.usci.eav.model.base.impl.BaseEntity;
import kz.bsbnb.usci.eav.model.base.impl.BaseSet;
import kz.bsbnb.usci.eav.model.exceptions.KnownException;
import kz.bsbnb.usci.eav.model.meta.IMetaAttribute;
import kz.bsbnb.usci.eav.model.meta.IMetaType;
import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;
import kz.bsbnb.usci.eav.model.meta.impl.MetaSet;
import kz.bsbnb.usci.eav.model.meta.impl.MetaValue;
import kz.bsbnb.usci.eav.model.type.ComplexKeyTypes;
import kz.bsbnb.usci.eav.persistance.db.JDBCSupport;
import kz.bsbnb.usci.eav.persistance.searcher.IBaseEntitySearcher;
import kz.bsbnb.usci.eav.tool.struct.StructType;
import kz.bsbnb.usci.eav.util.Errors;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static kz.bsbnb.eav.persistance.generated.Tables.*;

@Component
public class ImprovedBaseEntitySearcher extends JDBCSupport implements IBaseEntitySearcher {
    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    private DSLContext context;

    private final Logger logger = LoggerFactory.getLogger(ImprovedBaseEntitySearcher.class);

    @Override
    public String getClassName() {
        return null;
    }

    @Override
    public Long findSingle(BaseEntity entity, Long creditorId) {
        if (entity.getId() > 0)
            return entity.getId();

        SelectConditionStep select = generateSQL(entity, creditorId, null);

        if (select != null) {
            logger.debug(select.toString());
            List<Map<String, Object>> rows = queryForListWithStats(select.getSQL(), select.getBindValues().toArray());

            if (rows.size() > 1)
                throw new IllegalStateException(Errors.compose(Errors.E83,entity.getMeta().getClassName()));

            if (rows.size() < 1)
                return null;

            return ((BigDecimal) rows.get(0).get("inner_id")).longValue();
        }

        return null;
    }

    @Override
    public ArrayList<Long> findAll(BaseEntity baseEntity, Long creditorId) {
        ArrayList<Long> result = new ArrayList<>();

        if (baseEntity.getValueCount() == 0)
            return result;

        SelectConditionStep select = generateSQL(baseEntity, creditorId, null);

        if (select != null) {
            List<Map<String, Object>> rows = queryForListWithStats(select.getSQL(), select.getBindValues().toArray());

            for (Map<String, Object> row : rows)
                result.add(((BigDecimal) row.get("inner_id")).longValue());
        }

        Collections.sort(result);

        return result;
    }

    @SuppressWarnings("unchecked")
    public SelectConditionStep generateSQL(IBaseEntity entity, Long creditorId, String entityName) {
        MetaClass metaClass = entity.getMeta();
        String entityAlias = (entityName == null ? "root" : "e_" + entityName);

        SelectJoinStep joins;

        if (entity.getMeta().parentIsKey()) {
            joins = context.selectDistinct(EAV_BE_ENTITIES.as(entityAlias).ID.as("inner_id")).from(EAV_BE_ENTITIES.as(entityAlias));
        } else {
            joins = context.select(EAV_BE_ENTITIES.as(entityAlias).ID.as("inner_id")).from(EAV_BE_ENTITIES.as(entityAlias));
        }

        if (metaClass == null)
            throw new IllegalArgumentException(Errors.compose(Errors.E176));

        Condition condition = null;

        boolean hasOptionalKey = false;

        /* For optional key attributes */
        for (String name : metaClass.getMemberNames()) {
            IMetaAttribute metaAttribute = metaClass.getMetaAttribute(name);
            IMetaType memberType = metaClass.getMemberType(name);

            IBaseValue baseValue = entity.getBaseValue(name);

            if (metaAttribute.isOptionalKey() || metaAttribute.isNullableKey()) {
                MetaValue metaValue = (MetaValue) memberType;
                Table simpleTable = StructType.getSimpleTableName(metaValue.getTypeCode());

                if (baseValue == null || baseValue.getValue() == null) {
                    if(metaAttribute.isNullableKey()) {
                        condition = DSL.notExists(DSL.selectFrom(simpleTable).where(simpleTable.field("ENTITY_ID").eq(EAV_BE_ENTITIES.as(entityAlias).ID))
                                .and(simpleTable.field("CREDITOR_ID").eq(creditorId))
                                .and(simpleTable.field("ATTRIBUTE_ID").eq(metaAttribute.getId())));
                    }
                    continue;
                }

                generateJoins(joins, entityAlias, name, memberType, creditorId, metaAttribute);

                Object simpleValue = StructType.getSimpleValue(metaValue.getTypeCode(), baseValue.getValue());

                String valueAlias = "v_" + name;

                condition = simpleTable.as(valueAlias).field("VALUE").eq(simpleValue);

                hasOptionalKey = !metaAttribute.isNullableKey();

                break;
            }
        }

        /* For key attributes */
        if (!hasOptionalKey) {
            for (String name : metaClass.getMemberNames()) {
                IMetaAttribute metaAttribute = metaClass.getMetaAttribute(name);
                IMetaType memberType = metaClass.getMemberType(name);

                IBaseValue baseValue = entity.getBaseValue(name);

                if (metaAttribute.isKey()) {
                    if (baseValue == null || baseValue.getValue() == null)
                        throw new KnownException(Errors.compose(Errors.E177, name, entity.getMeta().getClassName()));

                    if (!memberType.isSet()) {
                        if (!memberType.isComplex()) {
                            generateJoins(joins, entityAlias, name, memberType, creditorId, metaAttribute);

                            MetaValue metaValue = (MetaValue) memberType;
                            Table simpleTable = StructType.getSimpleTableName(metaValue.getTypeCode());
                            Object simpleValue = StructType.getSimpleValue(metaValue.getTypeCode(), baseValue.getValue());

                            if (metaClass.getComplexKeyType() == ComplexKeyTypes.ALL) {
                                String valueAlias = "v_" + name;
                                condition = condition == null ?
                                        simpleTable.as(valueAlias).field("VALUE").eq(simpleValue) :
                                        condition.and(simpleTable.as(valueAlias).field("VALUE").eq(simpleValue));
                            } else {
                                String valueAlias = "v";
                                Select select = context
                                        .select(simpleTable.as(valueAlias).field("ID"))
                                        .from(simpleTable.as(valueAlias))
                                        .where(simpleTable.as(valueAlias).field("ENTITY_ID").eq(EAV_BE_ENTITIES.as(entityAlias).ID)
                                                .and(simpleTable.as(valueAlias).field("VALUE").eq(simpleValue)));

                                condition = condition == null ? DSL.exists(select) : condition.or(DSL.exists(select));
                            }
                        } else {
                            BaseEntity childBaseEntity = (BaseEntity) baseValue.getValue();
                            Long childBaseEntityId = childBaseEntity.getId();

                            if (childBaseEntityId == 0L) {
                                if (metaClass.getComplexKeyType() == ComplexKeyTypes.ALL)
                                    return null;
                            } else {
                                generateJoins(joins, entityAlias, name, memberType, creditorId, metaAttribute);

                                if (metaClass.getComplexKeyType() == ComplexKeyTypes.ALL) {
                                    String valueAlias = "v_" + name;
                                    condition = condition == null ?
                                            EAV_BE_COMPLEX_VALUES.as(valueAlias).ENTITY_VALUE_ID.equal(childBaseEntityId) :
                                            condition.and(EAV_BE_COMPLEX_VALUES.as(valueAlias).ENTITY_VALUE_ID.equal(childBaseEntityId));
                                } else {
                                    String valueAlias = "v";
                                    Select select = context
                                            .select(EAV_BE_COMPLEX_VALUES.as(valueAlias).ID).from(EAV_BE_COMPLEX_VALUES.as(valueAlias))
                                            .where(EAV_BE_COMPLEX_VALUES.as(valueAlias).ENTITY_ID.equal(EAV_BE_ENTITIES.as(entityAlias).ID)
                                                    .and(EAV_BE_COMPLEX_VALUES.as(valueAlias).ENTITY_VALUE_ID.equal(childBaseEntityId)));

                                    condition = condition == null ? DSL.exists(select) : condition.or(DSL.exists(select));
                                }
                            }
                        }
                    } else {
                        BaseSet baseSet = (BaseSet) baseValue.getValue();
                        MetaSet metaSet = (MetaSet) memberType;
                        MetaClass childMetaClass = (MetaClass) metaSet.getMemberType();

                        if (baseSet == null || baseSet.get().size() == 0)
                            throw new UnsupportedOperationException(Errors.compose(Errors.E178, (childMetaClass).getClassName()));

                        if (!memberType.isComplex())
                            throw new UnsupportedOperationException(Errors.compose(Errors.E179, childMetaClass.getClassName()));

                        List<Long> childBaseEntityIds = new ArrayList<>();

                        for (IBaseValue childBaseValue : baseSet.get()) {
                            BaseEntity childBaseEntity = (BaseEntity) childBaseValue.getValue();

                            if (childBaseEntity.getId() > 0) {
                                childBaseEntityIds.add(childBaseEntity.getId());
                            } else {
                                if (metaSet.getArrayKeyType() == ComplexKeyTypes.ALL)
                                    return null;
                            }
                        }

                        /* Ни один элемент ключевого массива не был идентифицирован */
                        if (childBaseEntityIds.size() == 0)
                            return null;

                        String className = childMetaClass.getClassName();
                        String setValueAlias = "sv_" + className;
                        String entitySetAlias = "es_" + className;
                        Select select;

                        if (metaSet.getArrayKeyType() == ComplexKeyTypes.ANY) {
                            select = context.select(EAV_BE_COMPLEX_SET_VALUES.as(setValueAlias).ENTITY_VALUE_ID)
                                    .from(EAV_BE_COMPLEX_SET_VALUES.as(setValueAlias))
                                    .join(EAV_BE_ENTITY_COMPLEX_SETS.as(entitySetAlias))
                                    .on(EAV_BE_ENTITY_COMPLEX_SETS.as(entitySetAlias).ATTRIBUTE_ID.eq(metaAttribute.getId()))
                                    .and(EAV_BE_COMPLEX_SET_VALUES.as(setValueAlias).CREDITOR_ID.eq(creditorId))
                                    .and(EAV_BE_COMPLEX_SET_VALUES.as(setValueAlias).SET_ID.eq(EAV_BE_ENTITY_COMPLEX_SETS.as(entitySetAlias).ID))
                                    .where(EAV_BE_ENTITY_COMPLEX_SETS.as(entitySetAlias).ENTITY_ID.eq(EAV_BE_ENTITIES.as(entityAlias).ID)
                                            .and(EAV_BE_COMPLEX_SET_VALUES.as(setValueAlias).ENTITY_VALUE_ID.in(childBaseEntityIds)));

                            condition = condition == null ? DSL.exists(select) :
                                    metaClass.getComplexKeyType() == ComplexKeyTypes.ALL ?
                                            condition.and(DSL.exists(select)) : condition.or(DSL.exists(select));
                        } else {
                            Collections.sort(childBaseEntityIds);
                            String sChildBaseEntityIds = StringUtils.arrayToDelimitedString(childBaseEntityIds.toArray(), ", ");

                            select = context.select(DSL.field("listagg(\"" + setValueAlias + "\".\"ENTITY_VALUE_ID\", ', ') " +
                                    "within group (order by \"" + setValueAlias + "\".\"ENTITY_VALUE_ID\" asc)"))
                                    .from(EAV_BE_COMPLEX_SET_VALUES.as(setValueAlias))
                                    .join(EAV_BE_ENTITY_COMPLEX_SETS.as(entitySetAlias))
                                    .on(EAV_BE_ENTITY_COMPLEX_SETS.as(entitySetAlias).ATTRIBUTE_ID.eq(metaAttribute.getId()))
                                    .and(EAV_BE_COMPLEX_SET_VALUES.as(setValueAlias).CREDITOR_ID.eq(creditorId))
                                    .and(EAV_BE_COMPLEX_SET_VALUES.as(setValueAlias).SET_ID.eq(EAV_BE_ENTITY_COMPLEX_SETS.as(entitySetAlias).ID))
                                    .where(EAV_BE_ENTITY_COMPLEX_SETS.as(entitySetAlias).ENTITY_ID.eq(EAV_BE_ENTITIES.as(entityAlias).ID));

                            Condition setCondition = select.asField().eq(sChildBaseEntityIds);

                            condition = condition == null ? setCondition :
                                    metaClass.getComplexKeyType() == ComplexKeyTypes.ALL ?
                                            condition.and(setCondition) : condition.or(setCondition);
                        }
                    }
                }
            }
        }

        if (entity.getMeta().parentIsKey()) {
            if (entity.getAddInfo().parentEntity == null || entity.getAddInfo().parentEntity.getId() == 0)
                return null;

            if (entity.getAddInfo().isSet) {
                final String ecs ="ebecs";
                final String cv = "ebcsv";

                joins.join(EAV_BE_ENTITY_COMPLEX_SETS.as(ecs)).on(EAV_BE_ENTITY_COMPLEX_SETS.as(ecs).ENTITY_ID.eq(entity.getAddInfo().parentEntity.getId()))
                        .and(EAV_BE_ENTITY_COMPLEX_SETS.as(ecs).ATTRIBUTE_ID.eq(entity.getAddInfo().attributeId))
                        .join(EAV_BE_COMPLEX_SET_VALUES.as(cv)).on(EAV_BE_ENTITY_COMPLEX_SETS.as(ecs).ID.eq(EAV_BE_COMPLEX_SET_VALUES.as(cv).SET_ID))
                            .and(EAV_BE_COMPLEX_SET_VALUES.as(cv).ENTITY_VALUE_ID.eq(EAV_BE_ENTITIES.as(entityAlias).ID));
            } else {
                final String cv = "ebcv";
                joins.join(EAV_BE_COMPLEX_VALUES.as(cv)).on(EAV_BE_COMPLEX_VALUES.as(cv).ENTITY_ID.eq(entity.getAddInfo().parentEntity.getId()))
                        .and(EAV_BE_COMPLEX_VALUES.as(cv).ATTRIBUTE_ID.eq(entity.getAddInfo().attributeId))
                        .and(EAV_BE_COMPLEX_VALUES.as(cv).ENTITY_VALUE_ID.eq(EAV_BE_ENTITIES.as(entityAlias).ID));
            }
        }

        SelectConditionStep where = joins.where(EAV_BE_ENTITIES.as(entityAlias).CLASS_ID.equal(metaClass.getId()));

        if (condition != null) {
            where = where.and(condition);
        } else {
            if(!entity.getMeta().parentIsKey())
                return null;
        }

        return where;
    }

    @SuppressWarnings("unchecked")
    private SelectJoinStep generateJoins(SelectJoinStep joins, String entityAlias, String name, IMetaType type,
                                         Long creditorId, IMetaAttribute attribute) {
        String valueAlias = "v_" + name;
        if (!type.isSet()) {
            if (!type.isComplex()) {
                MetaValue metaValue = (MetaValue) type;
                Table simpleTable = StructType.getSimpleTableName(metaValue.getTypeCode());

                joins = joins.join(simpleTable.as(valueAlias))
                        .on(EAV_BE_ENTITIES.as(entityAlias).ID.equal(simpleTable.as(valueAlias).field("ENTITY_ID"))
                                .and(simpleTable.as(valueAlias).field("CREDITOR_ID").eq(creditorId)
                                        .and(simpleTable.as(valueAlias).field("ATTRIBUTE_ID").eq(attribute.getId()))));
            } else {
                joins = joins.join(EAV_BE_COMPLEX_VALUES.as(valueAlias))
                        .on(EAV_BE_ENTITIES.as(entityAlias).ID.equal(EAV_BE_COMPLEX_VALUES.as(valueAlias).ENTITY_ID)
                                .and(EAV_BE_COMPLEX_VALUES.as(valueAlias).CREDITOR_ID.equal(creditorId))
                                .and(EAV_BE_COMPLEX_VALUES.as(valueAlias).ATTRIBUTE_ID.equal(attribute.getId())));
            }
        }

        return joins;
    }
}

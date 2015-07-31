package kz.bsbnb.usci.eav.persistance.searcher.impl;

import kz.bsbnb.usci.eav.model.base.impl.BaseEntity;
import kz.bsbnb.usci.eav.persistance.db.JDBCSupport;
import kz.bsbnb.usci.eav.persistance.searcher.IBaseEntitySearcher;
import kz.bsbnb.usci.eav.persistance.searcher.pool.impl.BasicBaseEntitySearcherPool;
import kz.bsbnb.usci.eav.util.DataUtils;
import org.jooq.DSLContext;
import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static kz.bsbnb.eav.persistance.generated.Tables.EAV_BE_COMPLEX_VALUES;
import static kz.bsbnb.eav.persistance.generated.Tables.EAV_BE_ENTITIES;

@Component
public class CreditSearcher extends JDBCSupport implements IBaseEntitySearcher {
    @Autowired
    private BasicBaseEntitySearcherPool searcherPool;

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    private DSLContext context;

    @Override
    public String getClassName() {
        return "credit";
    }

    @Override
    public Long findSingle(BaseEntity entity) {
        if (entity.getId() > 0)
            return entity.getId();

        List<Long> ids = searcherPool.getSearcher(entity.getMeta().getClassName()).findAll(entity);

        if (ids.size() > 1)
            throw new RuntimeException("Найдено более одного договора;");

        if (ids.size() < 1)
            return null;

        entity.setId(ids.get(0));

        return ids.get(0);
    }

    @Override
    public ArrayList<Long> findAll(BaseEntity entity) {
        ArrayList<Long> res = new ArrayList<>();

        BaseEntity primaryContract = (BaseEntity) entity.getEl("primary_contract");
        BaseEntity creditor = (BaseEntity) entity.getEl("data_creditor");

        if (primaryContract != null && creditor != null) {
            Long primaryContractId;
            Long creditorId;

            if (primaryContract.getId() > 0) {
                primaryContractId = primaryContract.getId();
            } else {
                primaryContractId = searcherPool.getSearcher(primaryContract.getMeta().
                        getClassName()).findSingle(primaryContract);
            }

            if (creditor.getId() > 0) {
                creditorId = creditor.getId();
            } else {
                creditorId = searcherPool.getSearcher(creditor.getMeta().
                        getClassName()).findSingle(creditor);
            }

            if (primaryContractId != null && creditorId != null && primaryContractId > 0 && creditorId > 0) {
                //String complexValuesTableAlias = "cv";

                SelectConditionStep select = (SelectConditionStep) context
                        .select(EAV_BE_ENTITIES.as("en").ID.as("inner_id"))
                        .from(EAV_BE_ENTITIES.as("en"))
                        .join(EAV_BE_COMPLEX_VALUES.as("co"))
                        .on(EAV_BE_ENTITIES.as("en").ID.equal(EAV_BE_COMPLEX_VALUES.as("co").ENTITY_ID))
                        .and(EAV_BE_ENTITIES.as("en").DELETED.eq(DataUtils.convert(true)))
                        .and(EAV_BE_COMPLEX_VALUES.as("co").ATTRIBUTE_ID.equal(entity.getMetaAttribute("primary_contract").getId()))
                        .and(EAV_BE_COMPLEX_VALUES.as("co").ENTITY_VALUE_ID.equal(primaryContractId))
                        .join(EAV_BE_COMPLEX_VALUES.as("co2"))
                        .on(EAV_BE_ENTITIES.as("en").ID.equal(EAV_BE_COMPLEX_VALUES.as("co2").ENTITY_ID))
                        .and(EAV_BE_COMPLEX_VALUES.as("co2").ATTRIBUTE_ID.equal(entity.getMetaAttribute("data_creditor").getId()))
                        .and(EAV_BE_COMPLEX_VALUES.as("co2").ENTITY_VALUE_ID.equal(creditorId));


                /*SelectConditionStep select = context
                        .select(EAV_BE_COMPLEX_VALUES.as(complexValuesTableAlias).ENTITY_ID.as("inner_id"))
                        .from(EAV_BE_COMPLEX_VALUES.as(complexValuesTableAlias))
                        .where(EAV_BE_COMPLEX_VALUES.as(complexValuesTableAlias).ATTRIBUTE_ID.
                                equal(entity.getMetaAttribute("primary_contract").getId()))
                        .and(EAV_BE_COMPLEX_VALUES.as(complexValuesTableAlias).ENTITY_VALUE_ID.
                                equal(primaryContractId))
                        .and(DSL.notExists(context.selectFrom(EAV_BE_ENTITIES).where(EAV_BE_ENTITIES.ID.
                                eq(EAV_BE_COMPLEX_VALUES.as(complexValuesTableAlias).ENTITY_ID)
                                .and(EAV_BE_ENTITIES.DELETED.eq(DataUtils.convert(true))))));
*/
                List<Map<String, Object>> rows = queryForListWithStats(select.getSQL(),
                        select.getBindValues().toArray());

                for (Map<String, Object> row : rows) {
                    long innerId = ((BigDecimal) row.get("inner_id")).longValue();
  /*                  SelectConditionStep selectInner = context
                            .select(EAV_BE_COMPLEX_VALUES.as(complexValuesTableAlias).ENTITY_ID.as("inner_id"))
                            .from(EAV_BE_COMPLEX_VALUES.as(complexValuesTableAlias))
                            .where(EAV_BE_COMPLEX_VALUES.as(complexValuesTableAlias).ATTRIBUTE_ID.
                                    equal(entity.getMetaAttribute("data_creditor").getId()))
                            .and(EAV_BE_COMPLEX_VALUES.as(complexValuesTableAlias).ENTITY_VALUE_ID.equal(creditorId))
                            .and(EAV_BE_COMPLEX_VALUES.as(complexValuesTableAlias).ENTITY_ID.equal(innerId));

                    if (queryForListWithStats(selectInner.getSQL(), selectInner.getBindValues().toArray()).size() <= 0)
                        continue;*/

                    res.add(innerId);
                }
            }
        }

        return res;
    }
}

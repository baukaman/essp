package kz.bsbnb.dao;

import kz.bsbnb.DataEntity;
import kz.bsbnb.dao.base.BaseDao;
import kz.bsbnb.usci.eav.model.meta.IMetaAttribute;
import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SearchEntityDao extends BaseDao {
    public long search(DataEntity entity) {
        MetaClass meta = entity.getMeta();
        StringBuilder buf = new StringBuilder(100);

        buf.append("SELECT DISTINCT(ENTITY_ID) FROM " + meta.getClassName());
        buf.append(" WHERE");

        List<Object> keys = new ArrayList<>();
        for (String attribute : entity.getAttributes()) {
            IMetaAttribute metaAttribute = meta.getMetaAttribute(attribute);
            if(metaAttribute.isKey()) {
                if(keys.size() > 0)
                    buf.append(" AND");
                buf.append(" ").append(safeColumnName(attribute) + " = ?");
                keys.add(entity.getBaseValue(attribute).getValue());
            }
        }

        List<Long> longs = jdbcTemplate.queryForList(buf.toString(), Long.class, keys.toArray());
        databaseActivity.select();

        if(longs.size() < 1)
            return 0;
        return longs.get(0);
    }
}

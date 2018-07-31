package kz.bsbnb.dao.impl;

import kz.bsbnb.DataEntity;
import kz.bsbnb.dao.ISearchEntityDao;
import org.springframework.stereotype.Component;

import java.util.List;

public class StaticSearchEntityDao implements ISearchEntityDao {

    protected List<DataEntity> refs;

    public StaticSearchEntityDao(List<DataEntity> refs) {
        this.refs = refs;
    }

    @Override
    public long search(DataEntity entity) {
        for (DataEntity ref : refs) {
            if(entity.equalsByKey(ref))
                return ref.getId();
        }

        return 0;
    }
}

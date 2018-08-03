package kz.bsbnb.engine.impl;

import kz.bsbnb.DataEntity;
import kz.bsbnb.dao.ISearchEntityDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class RefEngineImpl extends AbstractRefEngineImpl {

    @Autowired
    ISearchEntityDao searchEntityDao;

    @Override
    public long getId(DataEntity entity) {
        return searchEntityDao.search(entity);
    }

    @Override
    public DataEntity getById(long id, Date reportDAte) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reloadCache() {
        throw new UnsupportedOperationException();
    }
}

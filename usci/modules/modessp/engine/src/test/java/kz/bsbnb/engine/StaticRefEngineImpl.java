package kz.bsbnb.engine;

import kz.bsbnb.DataEntity;
import kz.bsbnb.engine.impl.AbstractRefEngineImpl;
import kz.bsbnb.exception.RefLoadException;
import kz.bsbnb.exception.RefNotFoundException;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;


@Primary
@Component
@Profile("dev")
public class StaticRefEngineImpl extends AbstractRefEngineImpl {

    List<DataEntity> cache;

    @Override
    public long getId(DataEntity entity) {
        if(cache != null) {
            for (DataEntity dataEntity : cache) {
                if (dataEntity.equalsByKey(entity)) {
                    return dataEntity.getId();
                }
            }
        }

        return -1;
    }

    public void initCache(List<DataEntity> data){
        this.cache = new LinkedList<>(data);
    }

    @Override
    public DataEntity getById(long id, Date reportDate) throws RefLoadException {
        for (DataEntity dataEntity : cache) {
            if(dataEntity.getId() == id)
                return dataEntity;
        }

        throw new RefLoadException(id, reportDate);
    }

    @Override
    public void reloadCache() {
        cache = null;
    }
}

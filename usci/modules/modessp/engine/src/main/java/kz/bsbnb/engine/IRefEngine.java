package kz.bsbnb.engine;

import kz.bsbnb.DataEntity;
import kz.bsbnb.exception.RefLoadException;
import kz.bsbnb.exception.RefNotFoundException;

import java.util.Date;

public interface IRefEngine {
    void process(DataEntity entity) throws RefNotFoundException;
    long getId(DataEntity entity);
    DataEntity getById(long id, Date reportDate) throws RefLoadException;
    void reloadCache();
}

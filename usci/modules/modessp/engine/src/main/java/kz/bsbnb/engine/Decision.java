package kz.bsbnb.engine;

import kz.bsbnb.DataEntity;
import kz.bsbnb.dao.DataEntityDao;
import org.springframework.beans.factory.annotation.Autowired;

public class Decision {
    protected DataEntity loadedEntity;
    protected DataEntity savingEntity;

    @Autowired
    DataEntityDao dataEntityDao;

    public Decision withLoaded(DataEntity loadEntity) {
        this.loadedEntity = loadEntity;
        return this;
    }

    public Decision withSaving(DataEntity savingEntity) {
        this.savingEntity = savingEntity;
        return this;
    }

    public DataEntity make() {
        return null;
    }
}

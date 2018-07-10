package kz.bsbnb.engine;

import kz.bsbnb.DataEntity;

public class Decision {
    protected DataEntity loadEntity;
    protected DataEntity savingEntity;

    public Decision withLoaded(DataEntity loadEntity) {
        this.loadEntity = loadEntity;
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

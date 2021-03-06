package kz.bsbnb.usci.eav.model.persistable.impl;

import kz.bsbnb.usci.eav.model.persistable.IPersistable;

public class Persistable implements IPersistable {
    public static final long DEFAULT_ID = 0;

    protected long id = 0;

    protected Persistable() {
        super();
    }

    protected Persistable(long id) {
        this.id = id;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Persistable)) return false;

        Persistable that = (Persistable) o;

        if (id != that.id) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}

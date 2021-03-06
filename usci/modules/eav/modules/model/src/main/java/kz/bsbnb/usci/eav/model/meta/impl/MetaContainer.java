package kz.bsbnb.usci.eav.model.meta.impl;

import kz.bsbnb.usci.eav.model.meta.IMetaContainer;

/**
 * @author alexandr.motov
 */
public abstract class MetaContainer extends MetaType implements IMetaContainer {

    private int type;

    protected MetaContainer(int type) {
        this.type = type;
    }

    protected MetaContainer(long id, int type) {
        super(id);
        this.type = type;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public void setType(int type) {
        this.type = type;
    }

}

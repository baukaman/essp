package kz.bsbnb.usci.eav.model.metadata.type.impl;

import kz.bsbnb.usci.eav.model.metadata.type.IMetaType;
import kz.bsbnb.usci.eav.persistance.Persistable;

public abstract class AbstractMetaType extends Persistable implements IMetaType/*, IPersistable*/ {
	/**
	 * <code>true</code> if attribute is a key attribute (used by DAO to find persisted entity)
	 * if an attribute has type DataTypes.COMPLEX then all it's key values will be used
	 * Defaults to <code>false</code>
	 */
	protected boolean isKey = false;
	/**
	 * <code>true</code> if attribute can have <code>null</code> value
	 * key attributes have this flag always set to false
	 * Defaults to <code>true</code> 
	 */
	protected boolean isNullable = true;
	
	public AbstractMetaType() {
	}

	public AbstractMetaType(boolean isKey, boolean isNullable) {
		this.isKey = isKey;
        this.isNullable = isNullable && !isKey;
	}

	@Override
	public boolean isKey() {
		return isKey;
	}

	@Override
	public void setKey(boolean isKey) {
		this.isKey = isKey;
		this.isNullable = isNullable && !isKey;
	}

	@Override
	public boolean isNullable() {
		return isNullable;
	}

	@Override
	public void setNullable(boolean isNullable) {
		this.isNullable = isNullable && !isKey;
	}
	
	
}
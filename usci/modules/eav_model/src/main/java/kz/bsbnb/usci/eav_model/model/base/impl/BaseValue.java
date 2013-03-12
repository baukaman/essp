package kz.bsbnb.usci.eav_model.model.base.impl;


import kz.bsbnb.usci.eav_model.model.Batch;
import kz.bsbnb.usci.eav_model.model.base.IBaseValue;
import kz.bsbnb.usci.eav_model.model.persistable.impl.Persistable;

import java.sql.Date;

/**
 * Attributes value place holder for BaseEntity. Contains information about batch and record number of the value's
 * origin and reference to the actual value as an instance of Object.
 *
 * @see kz.bsbnb.usci.eav_model.model.base.impl.BaseEntity
 *
 * @author a.motov
 */
public class BaseValue extends Persistable implements IBaseValue
{
    /**
     * Information about the sequential number of record in the batch
     */
    private long index;

    /**
     * Information about the origin of this value.
     */
    private Batch batch;

    /**
     * Can be a simple type, an array or a complex type.
     */
    private Object value;

    private Date repDate;

    /**
     * Initializes batch value with a batch information, index and value.
     * @param batch information about the origin of this value.
     * @param index the index of the value
     * @param value the value. May be is null.
     * @throws IllegalArgumentException if <code>Batch</code> is null or <code>Batch</code> has no id
     */
    public BaseValue(Batch batch, long index, Date repDate, Object value)
    {
        if (repDate == null)
            throw new IllegalArgumentException
                    ("repDate is null. Initialization of the BaseValue ​​is not possible.");

        if (batch == null)
            throw new IllegalArgumentException
                    ("Batch is null. Initialization of the BaseValue ​​is not possible.");

        if (batch.getId() < 1)
            throw new IllegalArgumentException
                    ("Batch has no id. Initialization of the BaseValue ​​is not possible.");


        this.batch = batch;
        this.index = index;
        this.value = value;
        this.repDate = repDate;
    }

    public BaseValue(Batch batch, long index, Object value)
    {
        if (batch == null)
            throw new IllegalArgumentException
                    ("Batch is null. Initialization of the BaseValue ​​is not possible.");

        if (batch.getId() < 1)
            throw new IllegalArgumentException
                    ("Batch has no id. Initialization of the BaseValue ​​is not possible.");


        this.batch = batch;
        this.index = index;
        this.value = value;
        this.repDate = batch.getRepDate();
    }

    @Override
    public Batch getBatch()
    {
        return batch;
    }

    @Override
    public long getIndex()
    {
        return index;
    }

    @Override
    public void setIndex(long index)
    {
        this.index = index;
    }

    @Override
    public Object getValue()
    {
        return value;
    }

    @Override
    public void setValue(Object value)
    {
        this.value = value;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
            return true;

        if (obj == null)
            return false;

        if (!(getClass() == obj.getClass()))
            return false;
        else
        {
            BaseValue that = (BaseValue)obj;

            return index == that.index && batch.equals(that.batch) && !(value != null ? !value.equals(that.value) : that.value != null);

        }
    }

    public Date getRepDate()
    {
        return repDate;
    }

    public void setRepDate(Date repDate)
    {
        this.repDate = repDate;
    }
}

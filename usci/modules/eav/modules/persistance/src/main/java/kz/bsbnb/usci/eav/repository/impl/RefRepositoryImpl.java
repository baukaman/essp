package kz.bsbnb.usci.eav.repository.impl;

import kz.bsbnb.usci.eav.model.base.IBaseEntity;
import kz.bsbnb.usci.eav.model.base.IBaseValue;
import kz.bsbnb.usci.eav.model.base.impl.BaseSet;
import kz.bsbnb.usci.eav.model.meta.IMetaAttribute;
import kz.bsbnb.usci.eav.model.meta.IMetaType;
import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;
import kz.bsbnb.usci.eav.model.meta.impl.MetaValue;
import kz.bsbnb.usci.eav.persistance.dao.ISQLGenerator;
import kz.bsbnb.usci.eav.repository.IMetaClassRepository;
import kz.bsbnb.usci.eav.repository.IRefRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Repository
public class RefRepositoryImpl implements IRefRepository, InitializingBean {
    private Map<Long, List<Map<String, Object>>> prepareMap = new HashMap<>();

    @Autowired
    private ISQLGenerator sqlGenerator;

    @Qualifier("metaClassRepositoryImpl")
    @Autowired
    private IMetaClassRepository metaClassRepository;

    private ReentrantReadWriteLock semaphore = new ReentrantReadWriteLock();

    @Override
    public void afterPropertiesSet() throws Exception {
        semaphore.writeLock().lock();
        try {
            long t1 = System.currentTimeMillis();
            for (MetaClass meta : metaClassRepository.getMetaClasses()) {
                if (meta.isReference())
                    prepareMap.put(meta.getId(), sqlGenerator.getSimpleResult(meta.getId(), true));
            }
            System.out.println("Caching time: " + (System.currentTimeMillis() - t1));
        } finally {
            semaphore.writeLock().unlock();
        }
    }

    @Override
    public long prepareRef(final IBaseEntity baseEntity) {
        semaphore.readLock().lock();
        try {
            List<Map<String, Object>> mapList = prepareMap.get(baseEntity.getMeta().getId());
            List<Map<String, Object>> currentEntityMapList = convert(baseEntity);

            for (Map<String, Object> map : mapList) {
                for (Map<String, Object> current : currentEntityMapList) {
                    boolean found = true;

                    for (Map.Entry<String, Object> currentEntry : current.entrySet()) {
                        if (!currentEntry.getValue().equals(map.get(currentEntry.getKey()))) {
                            found = false;
                            break;
                        }
                    }

                    if (found)
                        return ((BigDecimal) map.get(baseEntity.getMeta().getClassName().toUpperCase() + "_ID")).longValue();
                }
            }

            return 0;
        } finally {
            semaphore.readLock().unlock();
        }
    }

    public void installRef(final IBaseEntity entity) {
        semaphore.writeLock().lock();
        try {
            List<Map<String, Object>> mapList = convert(entity);

            List<Map<String, Object>> preparedRef = prepareMap.get(entity.getMeta().getId());

            for (Map<String, Object> m : mapList) {
                m.put(entity.getMeta().getClassName().toUpperCase() + "_ID", new BigDecimal(entity.getId()));
                preparedRef.add(m);
            }
        } finally {
            semaphore.writeLock().unlock();
        }
    }

    private List<Map<String, Object>> convert (IBaseEntity baseEntity) {
        List<Map<String, Object>> mapList = new LinkedList<>();

        for (String attributeName : baseEntity.getMeta().getAttributeNames()) {
            IMetaAttribute metaAttribute = baseEntity.getMeta().getMetaAttribute(attributeName);
            IMetaType metaType = metaAttribute.getMetaType();

            if (!metaAttribute.isKey() && !metaAttribute.isOptionalKey())
                continue;

            if (metaType.isSet() && metaType.isComplex()) {
                final IBaseValue baseValue = baseEntity.getBaseValue(attributeName);

                if (baseValue == null)
                    continue;

                final BaseSet baseSet = (BaseSet) baseValue.getValue();

                for (IBaseValue childBaseValue : baseSet.get()) {
                    Map<String, Object> map = new HashMap<>();
                    IBaseEntity childBaseEntity = (IBaseEntity) childBaseValue.getValue();
                    map.put(childBaseEntity.getMeta().getClassName().toUpperCase() + "_ID", new BigDecimal(childBaseEntity.getId()));
                    mapList.add(map);
                }
            }
        }

        if (mapList.size() == 0) {
            mapList.add(new HashMap<String, Object>());
        }

        for (String attributeName : baseEntity.getMeta().getAttributeNames()) {
            IMetaAttribute metaAttribute = baseEntity.getMeta().getMetaAttribute(attributeName);
            IMetaType metaType = metaAttribute.getMetaType();

            if ((!metaAttribute.isKey() && !metaAttribute.isOptionalKey()) || metaType.isSet())
                continue;

            final String attrName = metaAttribute.getName().toUpperCase();
            final IBaseValue baseValue = baseEntity.getBaseValue(attributeName);

            if (metaAttribute.isOptionalKey() && (baseValue == null || baseValue.getValue() == null))
                continue;

            String attrNewName;
            Object value;

            if (metaType.isComplex()) {
                attrNewName = attrName + "_ID";
                value = new BigDecimal(((IBaseEntity) baseValue.getValue()).getId());
            } else {
                attrNewName = attrName;
                MetaValue metaValue = (MetaValue) metaType;

                switch(metaValue.getTypeCode()) {
                    case INTEGER:
                        value = new BigDecimal(String.valueOf(baseValue.getValue()));
                        break;
                    default:
                        value = baseValue.getValue();
                        break;
                }
            }

            for (Map<String, Object> entry : mapList) {
                entry.put(attrNewName, value);
            }
        }

        return mapList;
    }
}
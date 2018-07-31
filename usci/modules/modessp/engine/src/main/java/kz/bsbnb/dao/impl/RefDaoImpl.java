package kz.bsbnb.dao.impl;

import kz.bsbnb.DataEntity;
import kz.bsbnb.dao.IRefDao;
import org.springframework.stereotype.Component;

@Component
public class RefDaoImpl implements IRefDao {
    @Override
    public boolean find(DataEntity refEntity) {
        return false;
    }
}

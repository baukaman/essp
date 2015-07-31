package kz.bsbnb.usci.eav.persistance.dao;

import kz.bsbnb.usci.eav.model.EavGlobal;

public interface IEavGlobalDao {
    Long insert(EavGlobal eavGlobal);

    void update(EavGlobal eavGlobal);

    void delete(Long id);

    EavGlobal get(String type, String code);

    EavGlobal get(Long id);
}

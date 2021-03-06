package kz.bsbnb.usci.eav.persistance.searcher;

import kz.bsbnb.usci.eav.model.base.impl.BaseEntity;

import java.util.ArrayList;

public interface IBaseEntitySearcher {
    String getClassName();

    Long findSingle(BaseEntity entity, Long creditorId);

    ArrayList<Long> findAll(BaseEntity entity, Long creditorId);
}

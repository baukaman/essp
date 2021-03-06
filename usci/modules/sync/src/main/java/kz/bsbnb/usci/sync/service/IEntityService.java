package kz.bsbnb.usci.sync.service;


import kz.bsbnb.usci.eav.model.RefColumnsResponse;
import kz.bsbnb.usci.eav.model.RefListResponse;
import kz.bsbnb.usci.eav.model.base.impl.BaseEntity;
import kz.bsbnb.usci.tool.status.SyncStatus;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author k.tulbassiyev
 */
public interface IEntityService {
    void process(List<BaseEntity> entities);

    BaseEntity load(long id);

    BaseEntity load(long id, Date date);

    RefListResponse getRefListResponse(long metaClassId, Date date, boolean withHis);

    int getQueueSize();

    SyncStatus getStatus();

    RefColumnsResponse getRefColumns(long metaClassId);

    RefListResponse getRefListApprox(long metaClassId);

    BaseEntity loadForDisplay(long entityId, Date reportDate);

    Set<Long> getFinishedBatches();

    void batchFinishedInReader(Long batchId);
}

package kz.bsbnb.usci.eav.persistance.dao;


import kz.bsbnb.usci.eav.model.Batch;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface IBatchDao {

    Batch load(long id);

    long save(Batch batch);

    List<Batch> getPendingBatchList();

    List<Batch> getBatchListToSign(long userId);

    List<Batch> getAll(Date repDate);

    Map<String,String> getEntityStatusParams(long entityStatusId);

    void addEntityStatusParam(long entityStatusId, String key, String value);

}

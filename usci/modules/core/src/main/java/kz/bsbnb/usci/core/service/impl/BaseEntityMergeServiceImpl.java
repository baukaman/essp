package kz.bsbnb.usci.core.service.impl;

import com.google.gson.stream.JsonReader;
import kz.bsbnb.usci.core.service.IBaseEntityMergeService;
import kz.bsbnb.usci.eav.manager.IBaseEntityMergeManager;
import kz.bsbnb.usci.eav.manager.impl.BaseEntityMergeManager;
import kz.bsbnb.usci.eav.manager.impl.MergeManagerKey;
import kz.bsbnb.usci.eav.model.base.IBaseEntity;
import kz.bsbnb.usci.eav.persistance.dao.IBaseEntityLoadDao;
import kz.bsbnb.usci.eav.persistance.dao.IBaseEntityMergeDao;
import kz.bsbnb.usci.eav.persistance.dao.IBaseEntityProcessorDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Date;

@Service
public class BaseEntityMergeServiceImpl implements IBaseEntityMergeService {
    @Autowired
    IBaseEntityProcessorDao baseEntityProcessorDao;

    @Autowired
    IBaseEntityLoadDao baseEntityLoadDao;

    @Autowired
    IBaseEntityMergeDao baseEntityMergeDao;

    @Override
    public void mergeBaseEntities(long leftEntityId, long rightEntityId, Date leftReportDate, Date rightReportDate,
                                  String json, boolean deleteUnused) {
        IBaseEntityMergeManager mergeManager = constructMergeManagerFromJson(json);

        IBaseEntity leftEntity = baseEntityLoadDao.loadByMaxReportDate(leftEntityId, leftReportDate);
        IBaseEntity rightEntity = baseEntityLoadDao.loadByMaxReportDate(rightEntityId, rightReportDate);

        if (mergeManager != null && leftEntity != null && rightEntity != null) {
            baseEntityMergeDao.merge(leftEntity, rightEntity, mergeManager,
                    IBaseEntityMergeDao.MergeResultChoice.LEFT, deleteUnused);
        }

    }

    private IBaseEntityMergeManager constructMergeManagerFromJson(String jsonStr) {
        IBaseEntityMergeManager mergeManager = new BaseEntityMergeManager();

        try {
            Reader reader = new StringReader(jsonStr);
            JsonReader jsonReader = new JsonReader(reader);
            jsonReader.beginObject();
            mergeManager = jsonToMergeManager(jsonReader);
            jsonReader.endObject();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return mergeManager;
    }

    public IBaseEntityMergeManager jsonToMergeManager(JsonReader jsonReader) throws IOException {
        IBaseEntityMergeManager mergeManager = new BaseEntityMergeManager();
        while (jsonReader.hasNext()) {
            String name = jsonReader.nextName();
            if (name.equals("action")) {
                String action = jsonReader.nextString();
                if (action.equals("keep_left"))
                    mergeManager.setAction(IBaseEntityMergeManager.Action.KEEP_LEFT);
                if (action.equals("keep_right"))
                    mergeManager.setAction(IBaseEntityMergeManager.Action.KEEP_RIGHT);
                if (action.equals("merge"))
                    mergeManager.setAction(IBaseEntityMergeManager.Action.TO_MERGE);
                if (action.equals("keep_both"))
                    mergeManager.setAction(IBaseEntityMergeManager.Action.KEEP_BOTH);
            } else if (name.equals("childMap")) {
                jsonReader.beginArray();
                while (jsonReader.hasNext()) {
                    MergeManagerKey key = null;
                    IBaseEntityMergeManager childManager = null;
                    jsonReader.beginObject();
                    while (jsonReader.hasNext()) {
                        String innerName = jsonReader.nextName();
                        if (innerName.equals("id")) {
                            jsonReader.beginObject();
                            key = getKeyFromJson(jsonReader);
                            jsonReader.endObject();
                        } else if (innerName.equals("map")) {
                            jsonReader.beginObject();
                            childManager = jsonToMergeManager(jsonReader);
                            jsonReader.endObject();
                        }

                    }
                    jsonReader.endObject();
                    mergeManager.setChildManager(key, childManager);
                }
                jsonReader.endArray();
            }
        }
        return mergeManager;
    }


    private MergeManagerKey getKeyFromJson(JsonReader jsonReader) {
        String type = null;
        String left = null;
        String right = null;
        String attr = null;
        try {
            while (jsonReader.hasNext()) {
                String name = jsonReader.nextName();
                if (name.equals("type")) {
                    type = jsonReader.nextString();
                } else if (name.equals("left")) {
                    left = jsonReader.nextString();
                } else if (name.equals("right")) {
                    right = jsonReader.nextString();
                } else if (name.equals("attr")) {
                    attr = jsonReader.nextString();
                }
            }
            if (type.equals("attribute")) {
                MergeManagerKey<String> key = new MergeManagerKey<String>(attr);
                return key;
            }
            if (type.equals("long")) {
                MergeManagerKey<Long> key = new MergeManagerKey<Long>(Long.parseLong(left), Long.parseLong(right));
                return key;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}

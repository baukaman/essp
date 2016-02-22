package kz.bsbnb.usci.eav.tool.optimizer.impl;

import kz.bsbnb.usci.eav.Errors;
import kz.bsbnb.usci.eav.model.base.IBaseEntity;
import kz.bsbnb.usci.eav.model.base.IBaseValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DocumentOptimizer {

    private static final Logger logger = LoggerFactory.getLogger(DocumentOptimizer.class);

    private DocumentOptimizer() {
    }

    public static String getKeyString(final IBaseEntity iBaseEntity) {
        StringBuilder stringBuilder = new StringBuilder();

        IBaseValue noBaseValue = iBaseEntity.getBaseValue("no");
        IBaseValue docTypeBaseValue = iBaseEntity.getBaseValue("doc_type");

        if (noBaseValue == null || docTypeBaseValue == null || noBaseValue.getValue() == null ||
                docTypeBaseValue.getValue() == null){
            logger.error(Errors.E184+" : \n" + iBaseEntity);
            throw new IllegalStateException(String.valueOf(Errors.E184));
        }


        IBaseEntity docTypeEntity = (IBaseEntity) docTypeBaseValue.getValue();

        if (docTypeEntity.getId() == 0){
            logger.error(Errors.E186+" : \n" + iBaseEntity);
            throw new IllegalStateException(String.valueOf(Errors.E186));
        }

        stringBuilder.append(noBaseValue.getValue());
        stringBuilder.append("|").append(docTypeEntity.getId());

        return stringBuilder.toString();
    }
}

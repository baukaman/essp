package kz.bsbnb.usci.eav.tool.optimizer.impl;

import kz.bsbnb.usci.eav.model.base.IBaseEntity;
import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;

import java.util.HashSet;
import java.util.Set;

public final class BasicOptimizer {
    public static final Set<String> metaList = new HashSet<>();

    static {
        metaList.add("subject");
        metaList.add("document");
        metaList.add("credit");
        metaList.add("primary_contract");
    }

    private BasicOptimizer() {
    }

    public static String getKeyString(final IBaseEntity iBaseEntity) {
        MetaClass meta = iBaseEntity.getMeta();

        switch (meta.getClassName()) {
            case "subject":
                return SubjectOptimizer.getKeyString(iBaseEntity);
            case "document":
                return DocumentOptimizer.getKeyString(iBaseEntity);
            case "primary_contract":
                return PrimaryContractOptimizer.getKeyString(iBaseEntity);
            case "credit":
                return CreditOptimizer.getKeyString(iBaseEntity);
            default:
                throw new IllegalStateException("Мета класс для оптимизаций не найден; \n" + iBaseEntity);
        }
    }
}
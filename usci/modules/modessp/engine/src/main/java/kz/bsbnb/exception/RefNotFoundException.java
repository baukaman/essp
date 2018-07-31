package kz.bsbnb.exception;

import kz.bsbnb.DataEntity;

public class RefNotFoundException extends RuntimeException {
    public RefNotFoundException(DataEntity refEntity) {

    }
}

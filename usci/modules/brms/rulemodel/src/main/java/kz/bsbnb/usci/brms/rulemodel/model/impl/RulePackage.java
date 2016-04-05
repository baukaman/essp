package kz.bsbnb.usci.brms.rulemodel.model.impl;

import kz.bsbnb.usci.eav.model.persistable.impl.Persistable;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Date;

/**
 * @author abukabayev
 */

@Component
public class RulePackage extends Persistable implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String name;
    private String description;


    public RulePackage(String name){
        this.name = name;
    }

    public RulePackage() {
    }

    public RulePackage(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "name: " + name + ", id: " + id;
    }
}

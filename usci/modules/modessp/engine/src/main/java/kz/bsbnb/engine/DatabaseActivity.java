package kz.bsbnb.engine;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "thread", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class DatabaseActivity {

    private int numberOfSelects = 0;
    private int numberOfInserts;
    protected int numberOfUpdates;

    public int numberOfSelects() {
        return numberOfSelects;
    }

    public int numberOfInserts() {
        return numberOfInserts;
    }

    public int numberOfUpdates() {
        return numberOfUpdates;
    }

    public void select() {
        numberOfSelects ++;
    }

    public void insert() {
        numberOfInserts++;
    }

    public void update() {
        numberOfUpdates++;
    }

    public DatabaseActivity() {
    }

    public void reset() {
        numberOfSelects = 0;
        numberOfInserts = 0;
        numberOfUpdates = 0;
    }
}

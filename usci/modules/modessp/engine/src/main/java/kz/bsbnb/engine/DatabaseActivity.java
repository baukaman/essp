package kz.bsbnb.engine;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "thread", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class DatabaseActivity {

    private int numberOfSelects = 0;

    public int numberOfSelects() {
        return numberOfSelects;
    }

    public int numberOfInserts() {
        return 0;
    }

    public int numberOfUpdates() {
        return 0;
    }

    public int noActions() {
        return 0;
    }

    public void select() {
        numberOfSelects ++;
    }

    public DatabaseActivity() {
        //System.out.println(Thread.currentThread().getName());
        //System.out.println("const");
    }

    public void reset() {
        numberOfSelects = 0;
    }
}

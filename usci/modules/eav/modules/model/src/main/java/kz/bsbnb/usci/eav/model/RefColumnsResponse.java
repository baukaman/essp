package kz.bsbnb.usci.eav.model;

import java.io.Serializable;
import java.util.List;

/**
 * Created by maksat on 6/1/15.
 */
public class RefColumnsResponse implements Serializable {

    private static final long serialVersionUID = 0L;

    private List<String> names;
    private List<String> titles;

    public RefColumnsResponse(List<String> names, List<String> titles) {
        this.names = names;
        this.titles = titles;
    }

    public List<String> getNames() {
        return names;
    }

    public List<String> getTitles() {
        return titles;
    }
}

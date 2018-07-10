package kz.bsbnb.dao.base;

import kz.bsbnb.dao.MetaClassDao;
import kz.bsbnb.engine.DatabaseActivity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

public class BaseDao {
    protected JdbcTemplate jdbcTemplate;

    protected MetaClassDao metaClassDao;

    @Autowired
    protected DatabaseActivity databaseActivity;

    @Autowired
    public void setDataSource(DataSource source){
        jdbcTemplate = new JdbcTemplate(source);
    }

    @Autowired
    public void setMetaSource(MetaClassDao metaClassDao) {
        this.metaClassDao = metaClassDao;
    }

    public String safeColumnName(String columnName){
        boolean addDash = columnName.equalsIgnoreCase("date");
        if(addDash)
            return columnName + "_";
        return columnName;
    }
}

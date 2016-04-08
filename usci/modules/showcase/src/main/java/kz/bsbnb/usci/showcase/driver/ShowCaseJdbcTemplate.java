package kz.bsbnb.usci.showcase.driver;

import kz.bsbnb.usci.eav.StaticRouter;
import kz.bsbnb.usci.eav.stats.QueryEntry;
import kz.bsbnb.usci.eav.stats.SQLQueriesStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;

@Component
public class ShowCaseJdbcTemplate {
    private JdbcTemplate jdbcTemplateSC;

    @Autowired
    private SQLQueriesStats sqlStats;

    @Autowired
    public void setDataSourceSC(DataSource dataSourceSC) {
        this.jdbcTemplateSC = new JdbcTemplate(dataSourceSC);
    }

    public Map<String, Object> queryForMap(String description, String sql, Object values[]) {
        if (StaticRouter.getStatsEnabled()) {
            long t1 = System.currentTimeMillis();
            Map<String, Object> map = jdbcTemplateSC.queryForMap(sql, values);
            sqlStats.put(description, (System.currentTimeMillis() - t1));
            return map;
        } else {
            return jdbcTemplateSC.queryForMap(sql, values);
        }
    }

    public int update(String description, String sql, Object values[]) {
        if(StaticRouter.getStatsEnabled()) {
            long t1 = System.currentTimeMillis();
            int rows = jdbcTemplateSC.update(sql, values);
            sqlStats.put(description, (System.currentTimeMillis() - t1));
            return rows;
        } else {
            return jdbcTemplateSC.update(sql, values);
        }
    }

    public Map<String, QueryEntry> getSqlStats() {
        return sqlStats.getStats();
    }
}
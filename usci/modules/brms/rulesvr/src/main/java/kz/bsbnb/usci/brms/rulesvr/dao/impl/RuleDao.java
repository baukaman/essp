package kz.bsbnb.usci.brms.rulesvr.dao.impl;

import kz.bsbnb.usci.brms.rulemodel.model.impl.BatchVersion;
import kz.bsbnb.usci.brms.rulemodel.model.impl.Rule;
import kz.bsbnb.usci.brms.rulemodel.model.impl.SimpleTrack;
import kz.bsbnb.usci.eav.util.DataUtils;
import kz.bsbnb.usci.eav.util.Errors;
import org.jooq.DSLContext;
import org.jooq.Delete;
import org.jooq.Select;
import org.jooq.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import kz.bsbnb.usci.brms.rulesvr.dao.IRuleDao;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static kz.bsbnb.usci.brms.rulesvr.generated.Tables.*;

/**
 * @author abukabayev
 */
public class RuleDao implements IRuleDao {

    private JdbcTemplate jdbcTemplate;
    private final String PREFIX_ = "LOGIC_";

    private final Logger logger = LoggerFactory.getLogger(RuleDao.class);

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    private DSLContext context;

    public boolean testConnection()
    {
        try
        {
            return !jdbcTemplate.getDataSource().getConnection().isClosed();
        }
        catch (SQLException e)
        {
            return false;
        }
    }


    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }


    public List<Rule> load(BatchVersion batchVersion){
        if (batchVersion.getId() < 1)
            throw new IllegalArgumentException(Errors.getMessage(Errors.E265));

        String SQL = "SELECT * FROM " + PREFIX_ + "rules WHERE id IN(SELECT rule_id FROM " + PREFIX_ +
                "rule_package_versions WHERE package_versions_id = ?)";

        List<Rule> ruleList = jdbcTemplate.query(SQL,new Object[]{batchVersion.getId()},
                new BeanPropertyRowMapper(Rule.class));

        return ruleList;
    }

    @Override
    public long update(Rule rule) {
        if (rule.getId() < 1){
            throw new IllegalArgumentException(Errors.getMessage(Errors.E266));
        }
        String SQL = "UPDATE " + PREFIX_ + "rules SET title=?, rule=? WHERE id=?";
        jdbcTemplate.update(SQL,rule.getTitle(),rule.getRule(),rule.getId());
        return rule.getId();
    }

    @Override
    public List<Rule> getAllRules() {
        String SQL = "SELECT * FROM " + PREFIX_ + "rules order by id";
        List<Rule> ruleList = jdbcTemplate.query(SQL,new BeanPropertyRowMapper(Rule.class));

        return ruleList;
    }


    @Override
    public List<SimpleTrack> getRuleTitles(Long packageId, Date repDate) {
        String SQL = "SELECT id, title AS NAME, is_active as isActive FROM " + PREFIX_ + "rules WHERE id IN (\n" +
                "SELECT rule_id FROM " + PREFIX_ + "rule_package_versions WHERE package_versions_id = \n" +
                "(SELECT id FROM " + PREFIX_ + "package_versions WHERE OPEN_DATE = \n" +
                "    (SELECT MAX(OPEN_DATE) FROM " + PREFIX_ + "package_versions WHERE package_id = ? AND OPEN_DATE <= ? ) \n" +
                " AND package_id = ? AND rownum = 1\n" +
                " )) order by id";


        List<SimpleTrack> ret = jdbcTemplate.query(SQL, new Object[]{packageId, repDate, packageId}, new BeanPropertyRowMapper(SimpleTrack.class));

        return ret;
    }

    @Override
    public List<SimpleTrack> getRuleTitles(Long batchVersionId, String searchText) {
        searchText = searchText.toLowerCase();
        Select select = context.select(LOGIC_RULES.ID, LOGIC_RULES.TITLE.as("name"), LOGIC_RULES.IS_ACTIVE, LOGIC_RULES.RULE)
                .from(LOGIC_RULES)
                .join(LOGIC_RULE_PACKAGE_VERSIONS).on(LOGIC_RULES.ID.eq(LOGIC_RULE_PACKAGE_VERSIONS.RULE_ID))
                .where(LOGIC_RULE_PACKAGE_VERSIONS.PACKAGE_VERSIONS_ID.eq(batchVersionId))
                .and(LOGIC_RULES.RULE.lower().like("%" + searchText + "%").or(LOGIC_RULES.TITLE.lower().like("%" + searchText + "%")));
        return jdbcTemplate.query(select.getSQL(), select.getBindValues().toArray(), new BeanPropertyRowMapper<SimpleTrack>(SimpleTrack.class));
    }

    public long save(Rule rule, BatchVersion batchVersion){
        String SQL = "INSERT INTO " + PREFIX_ + "rules(title, rule) VALUES(?, ?)";
        jdbcTemplate.update(SQL,rule.getTitle(),rule.getRule());

        SQL = "SELECT id FROM " + PREFIX_ + "rules WHERE rule = ?";
        long id = jdbcTemplate.queryForLong(SQL,rule.getRule());

//        if (batchVersion.getId() > 0){
//            SQL = "Insert into rule_package_versions(rule_id,package_versions_id) values(?,?)";
//            jdbcTemplate.update(SQL,id,batchVersion.getId());
//        }

        return id;
    }

    @Override
    public Rule getRule(Long ruleId) {
        String SQL = "SELECT * FROM " + PREFIX_ + "rules WHERE id = ?";
        List<Rule> rules = jdbcTemplate.query(SQL, new Object[]{ruleId}, new BeanPropertyRowMapper<Rule>(Rule.class));
        if(rules.size() > 1)
            throw new RuntimeException(Errors.getMessage(Errors.E264));
        return rules.get(0);
    }

    @Override
    public boolean deleteRule(long ruleId, long batchVersionId) {
        jdbcTemplate.update("DELETE FROM " + PREFIX_ + "rule_package_versions WHERE rule_id = ? " +
                "AND package_versions_id = ?", ruleId, batchVersionId);
        return true;
    }

    @Override
    public long createEmptyRule(final String title){
        KeyHolder keyHolder = new GeneratedKeyHolder();
        final String defaultRuleBody = "rule \" rule_" + (new Random().nextInt()) + "\"\nwhen\nthen\nend";
        jdbcTemplate.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                PreparedStatement ps = connection.prepareStatement("INSERT INTO " + PREFIX_ + "rules (title, rule) " +
                        "VALUES(?, ?)", new String[]{"id"});
                ps.setString(1,title);
                ps.setString(2, defaultRuleBody);
                return ps;
            }
        },keyHolder);
        return keyHolder.getKey().longValue();
    }

    @Override
    public void save(long ruleId, long batchVersionId) {
        String sql = "INSERT INTO " + PREFIX_ + "rule_package_versions (rule_id , package_versions_id) VALUES (?,?)";
        jdbcTemplate.update(sql,new Object[]{ruleId,batchVersionId});
    }

    @Override
    public void updateBody(Long ruleId, String body) {
        String sql = "UPDATE " + PREFIX_ + "rules SET rule = ? WHERE id=?";
        jdbcTemplate.update(sql, new Object[]{body, ruleId});
    }

    @Override
    public void copyExistingRule(long ruleId, long batchVersionId) {
        String sql = "INSERT INTO " + PREFIX_ + "rule_package_versions (rule_id, package_versions_id) VALUES (?,?)";
        jdbcTemplate.update(sql, new Object[]{ruleId, batchVersionId});
    }

    @Override
    public long createCopy(final long ruleId, final String title) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                PreparedStatement ps = connection.prepareStatement("INSERT INTO " + PREFIX_ + "rules (rule, title)\n" +
                        "  SELECT rule, ? AS title FROM rules WHERE id = ?", new String[]{"id"});
                ps.setString(1, title);
                ps.setLong(2,ruleId);
                return ps;
            }
        },keyHolder);

        return keyHolder.getKey().longValue();
    }

    @Override
    public long createRule(final Rule rule) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                PreparedStatement ps = connection.prepareStatement("INSERT INTO " + PREFIX_ + "rules (title, rule)" +
                        "values (?, ?)", new String[]{"id"});
                ps.setString(1, rule.getTitle());
                ps.setString(2, rule.getRule());
                return ps;
            }
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    @Override
    public void renameRule(long ruleId, String title) {
        String sql = "Update " + PREFIX_ + "rules set title = ? where id = ?";
        jdbcTemplate.update(sql, title, ruleId);
    }

    @Override
    public boolean activateRule(String ruleBody, long ruleId) {
        Update update = context.update(LOGIC_RULES)
                .set(LOGIC_RULES.RULE, ruleBody)
                .set(LOGIC_RULES.IS_ACTIVE, DataUtils.convert(true))
                .where(LOGIC_RULES.ID.eq(ruleId));


        try{
            jdbcTemplate.update(update.getSQL(), update.getBindValues().toArray());
        } catch(Exception e) {
            System.out.println(e.getMessage());
            return false;
        }

        return true;
    }

    @Override
    public boolean activateRule(long ruleId) {
        Update update = context.update(LOGIC_RULES)
                .set(LOGIC_RULES.IS_ACTIVE, DataUtils.convert(true))
                .where(LOGIC_RULES.ID.eq(ruleId));


        try{
            jdbcTemplate.update(update.getSQL(), update.getBindValues().toArray());
        } catch(Exception e) {
            System.out.println(e.getMessage());
            return false;
        }

        return true;
    }

    @Override
    public boolean disableRule(long ruleId) {
        Update update = context.update(LOGIC_RULES)
                .set(LOGIC_RULES.IS_ACTIVE, DataUtils.convert(false))
                .where(LOGIC_RULES.ID.eq(ruleId));

        try{
            jdbcTemplate.update(update.getSQL(), update.getBindValues().toArray());
        } catch(Exception e) {
            System.out.println(e.getMessage());
            return false;
        }

        return true;
    }

    @Override
    public void clearAllRules() {
        Delete delete = context.delete(LOGIC_RULE_PACKAGE_VERSIONS);
        jdbcTemplate.update(delete.getSQL());

        delete = context.delete(LOGIC_RULES);
        jdbcTemplate.update(delete.getSQL());
    }
}

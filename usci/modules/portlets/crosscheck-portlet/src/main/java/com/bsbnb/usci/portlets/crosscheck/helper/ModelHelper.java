package com.bsbnb.usci.portlets.crosscheck.helper;

import com.bsbnb.usci.portlets.crosscheck.dm.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ModelHelper {
    public static Creditor convertToCreditor(Connection conn, ResultSet rs) throws SQLException {
        Creditor c = new Creditor();
        c.setId(rs.getBigDecimal("ID").toBigInteger());
        c.setChangeDate(rs.getDate("CHANGE_DATE"));
        c.setCode(rs.getString("CODE"));
        c.setName(rs.getString("NAME"));
        c.setShortName(rs.getString("SHORT_NAME"));
        c.setShutdownDate(rs.getDate("SHUTDOWN_DATE"));
        // c.setMainOfficeId();
         c.setSubjectType(DbHelper.getSubjectType(conn, rs.getBigDecimal("SUBJECT_TYPE_ID")));

        return c;
    }

    public static CrossCheck convertToCrossCheck(ResultSet rs, Creditor c) throws SQLException {
        CrossCheck cc = new CrossCheck();
        cc.setId(rs.getBigDecimal("ID"));
        cc.setUsername(rs.getString("USER_NAME"));
        cc.setStatus(rs.getInt("STATUS_ID"));
        cc.setStatusName(rs.getString("STATUS_NAME"));
        cc.setDateBegin(rs.getTimestamp("DATE_BEGIN"));
        cc.setDateEnd(rs.getTimestamp("DATE_END"));
        cc.setReportDate(rs.getDate("REPORT_DATE"));
        cc.setCreditor(c);

        return cc;
    }

    public static CrossCheckMessage convertToCrossCheckMessage(ResultSet rs, CrossCheck cc, Message m) throws SQLException {
        CrossCheckMessage cm = new CrossCheckMessage();

        cm.setCrossCheck(cc);
        cm.setDescription(rs.getString("DESCRIPTION"));
        cm.setDiff(rs.getString("DIFF"));
        cm.setId(rs.getBigDecimal("ID").toBigInteger());
        cm.setInnerValue(rs.getString("INNER_VALUE"));
        cm.setIsError(rs.getBigDecimal("IS_ERROR").toBigInteger());
        cm.setMessage(m);
        cm.setOuterValue(rs.getString("OUTER_VALUE"));

        return cm;
    }

    public static Message convertToMessage(ResultSet rs) throws SQLException {
        Message m = new Message();

        m.setId(rs.getBigDecimal("ID"));
        m.setCode(rs.getString("CODE"));
        m.setNameKz(rs.getString("NAME_KZ"));
        m.setNameRu(rs.getString("NAME_RU"));
        m.setNote(rs.getString("NOTE"));

        return m;
    }

    public static SubjectType convertToSubjectType(ResultSet rs) throws SQLException {
        SubjectType st = new SubjectType();

        st.setCloseDate(rs.getDate("CLOSE_DATE"));
        st.setCode(rs.getString("CODE"));
        //st.setCreditorList();
        st.setId(rs.getBigDecimal("ID"));
        st.setIsLast(new Short("1"));
        //st.setKindId(rs.getBigDecimal("KIND_ID"));
        st.setNameKz(rs.getString("NAME_KZ"));
        st.setNameRu(rs.getString("NAME_RU"));
        st.setOpenDate(rs.getDate("OPEN_DATE"));
        //st.setParentId();
        st.setReportPeriodDurationMonths(rs.getInt("REPORT_PERIOD_DURATION_MONTHS"));
        //st.setSubjectTypeList();

        return st;
    }
}

package com.bsbnb.usci.portlets.crosscheck.helper;

import com.bsbnb.usci.portlets.crosscheck.dm.Creditor;
import com.bsbnb.usci.portlets.crosscheck.dm.CrossCheck;
import com.bsbnb.usci.portlets.crosscheck.dm.Message;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

import static com.bsbnb.usci.portlets.crosscheck.CrossCheckApplication.log;

/**
 * Created by ktulbassiyev on 11/20/14.
 */
public class DbHelper {
    public static Creditor getCreditor(Connection conn, BigInteger id) {
        Statement stmt = null;
        String query = "SELECT ID, MAIN_OFFICE_ID, SUBJECT_TYPE, NAME, SHORT_NAME, CODE, " +
                "SHUTDOWN_DATE, CHANGE_DATE, NOKBDB_CODE FROM R_REF_CREDITOR WHERE ID = " + id;

        // log.log(Level.INFO, "getCreditor: " + query);

        try {
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            rs.next();

            return ModelHelper.convertToCreditor(rs);

        } catch (SQLException e ) {
            e.printStackTrace();
        } finally {
            try {
                if(stmt!=null) {stmt.close();}
            } catch (SQLException sqle) {
                log.log(Level.WARNING, "Failed to cleanup", sqle);
            }
        }

        return null;
    }

    public static CrossCheck getCrossCheck(Connection conn, BigInteger id) {
        Statement stmt = null;
        String query = "SELECT ID, DATE_BEGIN, DATE_END, CREDITOR_ID, REPORT_DATE, STATUS_ID, USER_NAME, STATUS_NAME " +
                "FROM CROSS_CHECK WHERE ID = " + id;

        // log.log(Level.INFO, "getCrossCheck: " + query);

        try {
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            rs.next();

            CrossCheck c =  ModelHelper.convertToCrossCheck(rs, getCreditor(conn,
                    rs.getBigDecimal("CREDITOR_ID").toBigInteger()));

        } catch (SQLException e ) {
            e.printStackTrace();
        } finally {
            try {
                if(stmt!=null) {stmt.close();}
            } catch (SQLException sqle) {
                log.log(Level.WARNING, "Failed to cleanup", sqle);
            }
        }

        return null;
    }

    public static Message getMessage(Connection conn, BigInteger id) {
        Statement stmt = null;
        String query = "SELECT ID, CODE, NAME_RU, NAME_KZ, NOTE FROM MESSAGE WHERE ID = " + id;

        // log.log(Level.INFO, "getMessage: " + query);

        try {
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            rs.next();

            return ModelHelper.convertToMessage(rs);

        } catch (SQLException e ) {
            e.printStackTrace();
        } finally {
            try {
                if(stmt!=null) {stmt.close();}
            } catch (SQLException sqle) {
                log.log(Level.WARNING, "Failed to cleanup", sqle);
            }
        }

        return null;
    }
}

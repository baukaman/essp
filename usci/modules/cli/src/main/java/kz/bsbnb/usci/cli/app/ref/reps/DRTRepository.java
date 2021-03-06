package kz.bsbnb.usci.cli.app.ref.reps;

import kz.bsbnb.usci.cli.app.ref.BaseRepository;
import kz.bsbnb.usci.cli.app.ref.refs.BACT;
import kz.bsbnb.usci.cli.app.ref.refs.BalanceAccount;
import kz.bsbnb.usci.cli.app.ref.refs.CreditType;
import kz.bsbnb.usci.cli.app.ref.refs.EconSector;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by Bauyrzhan.Makhambeto on 13/06/2015.
 */
public class DRTRepository extends BaseRepository {
    /*private static HashMap repository;
    private static HashSet columns;
    //private static String QUERY = "SELECT * FROM ref.ba_ct t" + " where t.open_date <= to_date('repDate', 'dd.MM.yyyy') \n"+
    //         "   and (t.close_date > to_date('repDate', 'dd.MM.yyyy') or t.close_date is null)";

    private static String QUERY = "select * from ref.shared where type_ = 'debt_remains_type'";

    private static String COLUMNS_QUERY = "SELECT * FROM all_tab_cols WHERE owner = 'REF' AND TABLE_NAME='BA_CT'";*/

    public DRTRepository() {
        QUERY_ALL = "select * from ref.shared where type_ = 'debt_remains_type'";
        QUERY_OPEN = "select * from ref.shared where type_ = 'debt_remains_type'";
        QUERY_CLOSE = "select * from ref.shared where type_ = 'debt_remains_type' where 1 = 2";
        COLUMNS_QUERY = "SELECT * FROM all_tab_cols WHERE owner = 'REF' AND TABLE_NAME='BA_CT'";
    }

    @Override
    public HashMap construct(String query){
        try {
            ResultSet rows = getStatement().executeQuery(query.replaceAll("repDate",repDate));

            HashMap hm = new HashMap();
            int i = 0;
            while(rows.next()){
                HashMap tmp = new HashMap();

                tmp.put("CODE", rows.getString("CODE"));
                tmp.put("NAME_KZ", rows.getString("NAME_KZ"));
                tmp.put("NAME_RU", rows.getString("NAME_RU"));

                EconSector drt = new EconSector(tmp);
                hm.put(i++, drt);
            }

            return hm;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public BACT getById(String id){
        return (BACT) getRepository().get(id);
    }

    public HashSet getColumns() {
        return null;
    }

    public void rc(){
        repository = null;
    }
}

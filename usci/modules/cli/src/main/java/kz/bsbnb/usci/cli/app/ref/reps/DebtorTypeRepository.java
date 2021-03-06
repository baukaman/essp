package kz.bsbnb.usci.cli.app.ref.reps;

import kz.bsbnb.usci.cli.app.ref.BaseRepository;
import kz.bsbnb.usci.cli.app.ref.refs.DebtorType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created with IntelliJ IDEA.
 * User: Bauyrzhan.Makhambeto
 * Date: 18.03.14
 * Time: 19:06
 * To change this template use File | Settings | File Templates.
 */
public class DebtorTypeRepository extends BaseRepository{
    private static HashMap repository;
    private static HashSet columns;
    private static String QUERY = "SELECT * FROM ref.debtor_type";
    private static String COLUMNS_QUERY = "SELECT * FROM all_tab_cols WHERE owner = 'REF' AND TABLE_NAME='DEBTOR_TYPE'";

    public HashMap getRepository() {
        if(repository==null)
            repository = construct();
        return repository;
    }

    public HashMap construct(){
        try {
            HashSet hs = getColumns();
            ResultSet rows = getStatement().executeQuery(QUERY.replaceAll("repDate",repDate));

            HashMap hm = new HashMap();

            while(rows.next()){
                HashMap tmp = new HashMap();
                //System.out.println(rows.getString("NAME_RU"));
                for(Object s: hs){
                    //System.out.println(s);
                    tmp.put((String)s,rows.getString((String)s));
                    //System.out.println(rows.getString((String)s));
                }
                DebtorType dt = new DebtorType(tmp);
                hm.put(dt.get(dt.getKeyName()),dt);
            }

            return hm;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public DebtorType getByCode(String code){
        for(Object v : getRepository().values()){
            if( ( (DebtorType) v ).get("CODE").equals(code) )
                return (DebtorType) v;
        }
    return null;
    }

    public void rc(){
        repository = null;
    }
}

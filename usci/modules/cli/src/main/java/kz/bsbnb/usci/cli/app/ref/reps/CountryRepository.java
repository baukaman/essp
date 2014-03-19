package kz.bsbnb.usci.cli.app.ref.reps;


import kz.bsbnb.usci.cli.app.ref.BaseRepository;
import kz.bsbnb.usci.cli.app.ref.refs.Country;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class CountryRepository extends BaseRepository {
    private static HashMap repository;
    private static HashSet columns;
    private static String QUERY = "SELECT * FROM ref.COUNTRY";
    private static String COLUMNS_QUERY = "SELECT * FROM all_tab_cols WHERE owner = 'REF' AND TABLE_NAME='COUNTRY'";

    public static HashMap getRepository() {
        if(repository ==null)
            repository = construct();
        return repository;
    }

    public static HashMap construct(){
        try {
            ResultSet rows = getStatement().executeQuery(QUERY);

            HashMap hm = new HashMap();
            while(rows.next()){
                HashSet hs = getColumns();
                HashMap tmp = new HashMap();
                //System.out.println(rows.getString("NAME_RU"));
                for(Object s: hs){
                    //System.out.println(s);
                    tmp.put((String)s,rows.getString((String)s));
                }
                Country dt = new Country(tmp);
                hm.put(dt.get(dt.getKeyName()),dt);
            }

            return hm;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Country[] getByProperty(String key,String value){
        Country [] ret = new Country[0];
        List<Country> list = new ArrayList<Country>();
        for(Object v: getRepository().values()){
            if(((Country) v).get(key) != null)
                if(((Country) v).get(key).equals(value)) list.add((Country)v);
        }
        return list.toArray(ret);
    }

    public static Country getById(String id){
        return (Country) getRepository().get(id);
    }

    public static HashSet getColumns() {
        try {
            if(columns ==null){
                ResultSet rows = getStatement().executeQuery(COLUMNS_QUERY);
                HashSet hs = new HashSet();
                while(rows.next()){
                    hs.add(rows.getString("column_name"));
                }
                return columns = hs;
            }
            return columns;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}

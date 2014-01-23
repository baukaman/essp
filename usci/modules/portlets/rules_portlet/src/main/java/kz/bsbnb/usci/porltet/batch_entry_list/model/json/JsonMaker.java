package kz.bsbnb.usci.porltet.batch_entry_list.model.json;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Bauyrzhan.Makhambeto
 * Date: 20.01.14
 * Time: 9:51
 * To change this template use File | Settings | File Templates.
 */
public class JsonMaker {

    private static Gson gson = new Gson();

    public static class JsonFormat{
        int totalCount = 0;
        List<Object> data;
        boolean success;


        public JsonFormat(List data) {
            this.data = data;
            totalCount = data.size();
            success = true;
        }
    }

    public static String getJson(List data){
        return gson.toJson(new JsonFormat(data));
    }

    public static String getJson(Object data){
        Map m = new HashMap();
        m.put("data",data);
        m.put("success",true);
        return gson.toJson(m);
    }
}

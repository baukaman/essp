package kz.bsbnb.usci.tool;

import java.util.*;

public class Quote{

    public static String addSlashes(String str){
        if(str == null) return "";

        StringBuffer s = new StringBuffer (str);
        for (int i = 0; i < s.length(); i++)
            if (s.charAt (i) == '\'' || s.charAt (i) == '\"')
                s.insert (i++, '\\');
        return s.toString();

    }
}

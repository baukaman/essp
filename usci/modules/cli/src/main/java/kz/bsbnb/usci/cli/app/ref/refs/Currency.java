
package kz.bsbnb.usci.cli.app.ref.refs;

import kz.bsbnb.usci.cli.app.ref.BaseRef;
import org.w3c.dom.Element;

import java.util.HashMap;


public class Currency extends BaseRef {

    public Currency(HashMap hm){
       super(hm);
    }

    @Override
    public void buildElement(Element root) {
        appendToElement(root,"code",hm.get("CODE"));
        appendToElement(root,"is_convertible",hm.get("IS_CONVERTIBLE"));
        appendToElement(root,"name_kz",hm.get("NAME_KZ"));
        appendToElement(root,"name_ru",hm.get("NAME_RU"));
        appendToElement(root,"rating",hm.get("RATING"));
        appendToElement(root,"short_name",hm.get("SHORT_NAME"));
    }
}


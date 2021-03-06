
package kz.bsbnb.usci.cli.app.ref.refs;

import kz.bsbnb.usci.cli.app.ref.BaseRef;
import org.w3c.dom.Element;

import java.util.HashMap;


public class Shared extends BaseRef {
    public Shared(HashMap hm){
        super(hm);
    }

    @Override
    public void buildElement(Element root) {
        appendToElement(root,"code",hm.get("CODE"));
        appendToElement(root,"name_kz",hm.get("NAME_KZ"));
        appendToElement(root,"name_ru",hm.get("NAME_RU"));
        appendToElement(root,"note",hm.get("NOTE"));
        appendToElement(root,"order_num",hm.get("ORDER_NUM"));
        appendToElement(root,"type_",hm.get("TYPE_"));
    }
}



package kz.bsbnb.usci.cli.app.ref.refs;

import kz.bsbnb.usci.cli.app.ref.BaseRef;
import org.w3c.dom.Element;

import java.util.HashMap;


public class Portfolio extends BaseRef {
    public Portfolio(HashMap hm){
        super(hm);
    }

    @Override
    public void buildElement(Element root) {
        appendToElement(root,"code",hm.get("CODE"));
        appendToElement(root,"name_kz",hm.get("NAME_KZ"));
        appendToElement(root,"name_ru",hm.get("NAME_RU"));

        if(hm.get("creditor")!=null){
            Element creditor = getDocument().createElement("creditor");
            root.appendChild(creditor);
            Creditor c = (Creditor) hm.get("creditor");
            c.buildElement(creditor);
        }
    }
}


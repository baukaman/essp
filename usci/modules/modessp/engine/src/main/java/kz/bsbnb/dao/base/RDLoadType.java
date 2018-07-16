package kz.bsbnb.dao.base;

public enum RDLoadType {
    BYMAX("MAX","<="),
    BYMIN("MIN", ">");

    final public String agr;
    final public String sign;

    private RDLoadType(String agr, String sign){
        this.agr = agr;
        this.sign = sign;
    }

}

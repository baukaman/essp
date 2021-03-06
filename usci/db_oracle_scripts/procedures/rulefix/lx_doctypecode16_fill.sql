CREATE OR REPLACE PROCEDURE LX_DOCTYPECODE16_FILL
AS
  BEGIN
    EXECUTE IMMEDIATE 'truncate table lx_doctypecode16##';
    EXECUTE IMMEDIATE 'truncate table lx_doctypecode16#';
    EXECUTE IMMEDIATE 'truncate table lx_doctypecode16';

    INSERT INTO CORE.LX_DOCTYPECODE16# (
      SELECT
        ebe.id,
        ebcsv.ENTITY_VALUE_ID ENTITY_ID,
        15                    ATTRIBUTE_ID
      FROM EAV_BE_ENTITIES ebe                                                                                --subject
        JOIN EAV_BE_ENTITY_COMPLEX_SETS ebecs
          ON ebecs.ENTITY_ID = ebe.ID AND ebecs.ATTRIBUTE_ID = 12 AND ebecs.IS_LAST = 1
        JOIN EAV_BE_COMPLEX_SET_VALUES ebcsv ON ebcsv.SET_ID = ebecs.ID
--docs
        JOIN eav_be_complex_values ebcv
          ON ebcv.ENTITY_ID = ebcsv.ENTITY_VALUE_ID AND ebcv.ATTRIBUTE_ID = 1 AND ebcv.IS_LAST = 1
--doc_type
        LEFT OUTER JOIN EAV_BE_STRING_VALUES ebsv
          ON ebsv.ENTITY_ID = ebcv.ENTITY_VALUE_ID AND ebsv.ATTRIBUTE_ID = 4 AND ebsv.IS_LAST = 1
-- code
        LEFT OUTER JOIN EAV_BE_STRING_VALUES ebsv2
          ON ebsv2.ENTITY_ID = ebcsv.ENTITY_VALUE_ID AND ebsv2.ATTRIBUTE_ID = 15 AND ebsv2.IS_LAST = 1 -- name
      WHERE ebe.CLASS_ID = 42 AND ebsv.VALUE = '16' AND ebsv2.VALUE IS NULL);

    INSERT INTO "CORE"."LX_DOCTYPECODE16##" (
      SELECT
        ebcv.ENTITY_VALUE_ID primary_conract_id,
        ebsv.value           pno,
        ebdv.value           pdate,
        ebsv.creditor_id,
        CORE.LX_DOCTYPECODE16#.ENTITY_ID,
        CORE.LX_DOCTYPECODE16#.attribute_id
      FROM eav_be_entities ebe                         --credit
        LEFT OUTER JOIN EAV_BE_COMPLEX_VALUES ebcv ON ebcv.ENTITY_ID = ebe.id AND ebcv.ATTRIBUTE_ID = 57
--primary_contract
        LEFT OUTER JOIN EAV_BE_STRING_VALUES ebsv ON ebsv.ENTITY_ID = ebcv.ENTITY_VALUE_ID AND ebsv.ATTRIBUTE_ID = 153
--primary_contract.number
        LEFT OUTER JOIN EAV_BE_DATE_VALUES ebdv ON ebdv.ENTITY_ID = ebcv.ENTITY_VALUE_ID AND ebdv.ATTRIBUTE_ID = 152
--primary_contract.date
        LEFT OUTER JOIN EAV_BE_COMPLEX_VALUES ebcv2 ON ebcv2.ENTITY_ID = ebe.id AND ebcv2.ATTRIBUTE_ID = 59
--subject
        JOIN CORE.LX_DOCTYPECODE16# ON CORE.LX_DOCTYPECODE16#.id = ebcv2.ENTITY_VALUE_ID
      WHERE
        ebe.CLASS_ID = 59);

    INSERT
    INTO LX_DOCTYPECODE16
      (
        SELECT
          "LX_DOCTYPECODE16##"."primary_conract_id",
          "LX_DOCTYPECODE16##"."ENTITY_ID",
          "LX_DOCTYPECODE16##"."ATTRIBUTE_ID",
          "LX_DOCTYPECODE16##"."creditor_id",
          rdc.name_ru
        FROM credit@credits c
          LEFT OUTER JOIN debtor@credits d ON d.credit_id = c.id
          LEFT OUTER JOIN v_debtor_doc_his@credits vdd ON CASE WHEN (d.org_id IS NOT NULL AND vdd.org_id = d.org_id) OR
                                                                    (d.person_id IS NOT NULL AND
                                                                     vdd.person_id = d.person_id) THEN 1
                                                          ELSE 0 END = 1
          LEFT OUTER JOIN REF.doc_type@credits rdc ON rdc.id = vdd.type_id
          JOIN v_creditor_map vcm ON vcm.creditor_id = c.creditor_id
          JOIN "LX_DOCTYPECODE16##" ON "LX_DOCTYPECODE16##"."pno" = c.primary_contract_no AND
                                       "LX_DOCTYPECODE16##"."pdate" = c.primary_contract_date AND
                                       "LX_DOCTYPECODE16##"."creditor_id" = vcm.ref_creditor_id
      );

  END LX_DOCTYPECODE16_FILL;
create or replace PROCEDURE "LX_INTER_PASTDUE_VAL1_FILL" IS
  v_primary_contract_no varchar(255);
  v_primary_contract_date date;
  v_creditor_id number;
BEGIN

  DELETE FROM LX_INTER_PASTDUE_VAL1;

  --GET DEPT AND PASTDUE
  for pastdues in (
      select pd.entity_id interestId, pd.entity_value_id pastdueId, pd.report_date reportDate,pv.value value, od.value openDate, cd.value closeDate
          from eav_be_complex_values pd
            left outer join eav_be_double_values pv on pv.entity_id = pd.entity_value_id and pv.attribute_id = 64
            left outer join eav_be_date_values cd on cd.entity_id = pd.entity_value_id and cd.attribute_id = 62
            left outer join eav_be_date_values od on od.entity_id = pd.entity_value_id and od.attribute_id = 63
          where
            pv.value = 0
            and od.value is not null
            and cd.value is null
  )
   loop
      insert into LX_INTER_PASTDUE_VAL1(pastdue_id, interest_id, report_date, value, open_date, close_date)
      values(pastdues.pastdueId, pastdues.interestId, pastdues.reportDate,  pastdues.value, pastdues.openDate, pastdues.closeDate);
   end loop;


   --GET REMAINS
   for remains in (
      SELECT ebe.id remain, clx.entity_value_id interest
      from eav_be_entities ebe
      left outer join eav_be_complex_values clx on ebe.id = clx.entity_id and clx.attribute_id = 42
      where ebe.class_id = 43 and clx.entity_value_id in (
          select interest_id  from LX_INTER_PASTDUE_VAL1
      )
  )
   loop
      update LX_INTER_PASTDUE_VAL1 lx set lx.remains_id=remains.remain where lx.interest_id=remains.interest;
   end loop;

  --GET CHANGES
  for changes in (
      SELECT ebe.id chng, clx.entity_value_id remain
      from eav_be_entities ebe
      left outer join eav_be_complex_values clx on ebe.id = clx.entity_id and clx.attribute_id = 55
      where ebe.class_id = 57 and clx.entity_value_id in  (
          select remains_id from LX_INTER_PASTDUE_VAL1
      )
  )
   loop
      update LX_INTER_PASTDUE_VAL1 lx set lx.change_id=changes.chng where lx.remains_id=changes.remain;
   end loop;


  --GET CREDITS
  for credits in (
      SELECT ebe.id credit, clx.entity_value_id chng
      from eav_be_entities ebe
      left outer join eav_be_complex_values clx on ebe.id = clx.entity_id and clx.attribute_id = 68
      where ebe.class_id = 59 and clx.entity_value_id in (
          select change_id from LX_INTER_PASTDUE_VAL1
      )
  )
   loop
      update LX_INTER_PASTDUE_VAL1 lx set lx.credit_id=credits.credit where lx.change_id=credits.chng;
   end loop;

  --GET CONTRACT NO and DATE
  for cr in (select * from LX_INTER_PASTDUE_VAL1)
   loop
      select primary_contract_no, primary_contract_date, creditor_id
        into v_primary_contract_no, v_primary_contract_date, v_creditor_id
        from r_core_credit@showcase sh
       where cr.credit_id = sh.credit_id;

      update LX_INTER_PASTDUE_VAL1 lx
         set lx.p_cont_no = v_primary_contract_no,
             lx.p_cont_date = v_primary_contract_date,
             lx.creditor_id = v_creditor_id
      where lx.credit_id = cr.credit_id;
   end loop;

END LX_INTER_PASTDUE_VAL1_FILL;
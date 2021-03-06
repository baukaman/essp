create or replace PROCEDURE "LX_INTER_PASTDUE_VAL2_FIX" IS
BEGIN

  for pd in (select * from LX_INTER_PASTDUE_VAL2)
   loop
      declare
      crValue number;
      crOpenDate date;
      v_cr_count number;
      begin

            SELECT dr.value, dr.pastdue_open_date
                INTO crValue, crOpenDate
            FROM credit@credits cr
              LEFT OUTER JOIN debt_remains@credits dr
                ON cr.id                    =dr.credit_id
            WHERE cr.PRIMARY_CONTRACT_NO=pd.p_cont_no
                AND cr.PRIMARY_CONTRACT_DATE=pd.p_cont_date
                AND dr.type_id              =59
                AND dr.rep_date             =pd.report_date;

            UPDATE LX_INTER_PASTDUE_VAL2 lx
                SET lx.cr_value=crValue
            WHERE
                lx.P_CONT_NO   =pd.p_cont_no
                AND lx.P_CONT_DATE   =pd.p_cont_date;

            UPDATE LX_INTER_PASTDUE_VAL2 lx
                SET lx.cr_open_date=crOpenDate
            WHERE
                lx.P_CONT_NO   =pd.p_cont_no
                AND lx.P_CONT_DATE   =pd.p_cont_date;


          IF crValue <> pd.cr_value THEN
              null;
              --update value
              --dbms_output.put_line('update eav_be_double_values set value='||crValue||' where entity_id = '||pd.pastdue_id||' and attribute_id = 64');
              --update eav_be_date_values
              --set value=crOpenDate
              --where entity_id = pd.pastdue_id
              --and attribute_id = 64;
          END IF;

          IF crOpenDate <> pd.open_date THEN
              null;
              --update open date
              --dbms_output.put_line('update eav_be_date_values set value='||crOpenDate||' where entity_id = '||pd.pastdue_id||' and attribute_id = 63');
              --update eav_be_date_values
              --set value=crOpenDate
              --where entity_id = pd.pastdue_id
              --and attribute_id = 63;
          END IF;

          IF crValue is null and  crValue <> 0 THEN
            null;
            --dbms_output.put_line('MKR value and open_date incorrect, can not fix for ESSP credit_id : '||pd.credit_id);
          END IF;

       EXCEPTION
        WHEN no_data_found THEN
          update LX_INTER_PASTDUE_VAL2 set cr_not_found=1 where pastdue_id=pd.pastdue_id;

          SELECT count(cr.id)
              into v_cr_count
            FROM credit@credits cr
            WHERE cr.PRIMARY_CONTRACT_NO=pd.p_cont_no
                AND cr.PRIMARY_CONTRACT_DATE=pd.p_cont_date;

            if v_cr_count > 0 then
              insert into lx_log(log_date,rule_name,log_message) values(sysdate, 'inter_pastdue_val2','delete pastdue from mkr  in credit_id : '||pd.credit_id);
              --delete from eav_be_date_values where attribute_id=63 and entity_id = pd.pastdue_id;
              --delete from eav_be_date_values where attribute_id=62 and entity_id = pd.pastdue_id;
              --delete from eav_be_double_values where attribute_id=64 and entity_id = pd.pastdue_id;
              --delete from eav_be_double_values where attribute_id=65 and entity_id = pd.pastdue_id;
              --delete from eav_be_complex_values where attribute_id=16 and entity_value_id = pd.pastdue_id;
            else
              insert into lx_log(log_date,rule_name,log_message) values(sysdate, 'inter_pastdue_val2','MKR credit not foud for ESSP credit_id : '||pd.credit_id);
            end if;
        WHEN TOO_MANY_ROWS THEN
          insert into lx_log(log_date,rule_name,log_message) values(sysdate, 'inter_pastdue_val2','MKR too many debt_remains, may be report_date problem for ESSP credit_id : '||pd.credit_id);
      end;
    end loop;

END LX_INTER_PASTDUE_VAL2_FIX;
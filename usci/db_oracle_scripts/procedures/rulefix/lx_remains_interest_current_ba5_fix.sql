CREATE OR REPLACE PROCEDURE LX_RINTEREST_CURRENT_BA5_FIX IS
  v_count number;
BEGIN

   select count(1) into v_count from lx_rinterest_current_ba5;

   if v_count > 0 THEN
      lx_write_log(sysdate, 'remains_interest_current_ba5', 'rule remains_interest_current_ba5: ' || v_count || ' credits failed');
      dbms_output.put_line('rule remains_interest_current_ba5: ' || v_count || ' credits failed');
   else
      lx_write_log(sysdate, 'remains_interest_current_ba5', 'rule lx_remains_interest_current_ba5 is ok !!!');
      dbms_output.put_line('rule lx_remains_interest_current_ba5 is ok !!!');
   end if;


END LX_RINTEREST_CURRENT_BA5_FIX;

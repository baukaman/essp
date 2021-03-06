/*
CREATE TABLE LX_E92EO_FIXER (
  "ID" NUMBER(14,0) primary key,
  "CREDITOR_ID" NUMBER(14,0),
  "META_ID" NUMBER(14,0),
  "ENTITY_ID" NUMBER(14,0),
  "KEY_STRING" VARCHAR2(128) NOT NULL)
*/

CREATE OR REPLACE PACKAGE PKG_E92EO_FIX
IS
  c_default_job_max_count CONSTANT NUMBER := 20;
  c_default_job_size      CONSTANT NUMBER := 1000000;
  PROCEDURE run;
  PROCEDURE run_as_job;
  PROCEDURE run_interval(
    p_start_index NUMBER,
    p_end_index   NUMBER);
  PROCEDURE write_log(
    p_message IN VARCHAR2);
END PKG_E92EO_FIX;
/



CREATE OR REPLACE PACKAGE BODY PKG_E92EO_FIX
IS
  PROCEDURE write_log(
    p_message IN VARCHAR2)
  IS
    BEGIN
      INSERT INTO lx_E92_log VALUES
        (seq_E92_log_id.nextval, p_message
        );
    END;
  PROCEDURE run_as_job
  IS
    BEGIN
      DELETE FROM lx_E92_worker;
      DELETE FROM lx_E92EO_fixer;
      dbms_scheduler.create_job(job_name => 'ES_E92EO_job_runner', job_type => 'PLSQL_BLOCK', job_action => 'BEGIN PKG_E92EO_FIX.RUN; END;', start_date => systimestamp, repeat_interval => NULL, enabled => true, auto_drop => true);
    END;
  PROCEDURE run
  IS
    v_job_count NUMBER;
    v_start_id  NUMBER;
    v_end_id    NUMBER;
    BEGIN
      WHILE(true)
      LOOP
        SELECT COUNT(*) INTO v_job_count FROM lx_E92_worker WHERE status = 'RUNNING';
        IF(v_job_count < c_default_job_max_count) THEN
          BEGIN
            SELECT NVL(MAX(end_id), 2) INTO v_start_id FROM lx_E92_worker;
            EXCEPTION
            WHEN no_data_found THEN
            v_start_id := 1;
          END;
          SELECT MAX(id)
          INTO v_end_id
          FROM
            (SELECT id FROM eav_optimizer WHERE id >= v_start_Id ORDER BY id
            )
          WHERE rownum <= c_default_job_size;
          IF(v_start_id = v_end_id) THEN
            EXIT;
          END IF;
          INSERT
          INTO lx_E92_worker
          (
            id,
            start_id,
            end_id,
            status,
            start_date
          )
          VALUES
            (
              seq_lx_E92_worker_id.nextval,
              v_start_Id,
              v_end_id,
              'RUNNING',
              systimestamp
            );
          dbms_scheduler.create_job( job_name => 'ES_JOB_E92EO_' || v_end_id, job_type => 'PLSQL_BLOCK', job_action => 'BEGIN PKG_E92EO_FIX.run_interval(p_start_index => '|| v_start_Id ||', p_end_index => '|| v_end_id || '); END;', start_date => systimestamp, repeat_interval => NULL, enabled =>true, auto_drop => true);
        ELSE
          dbms_lock.sleep(5);
        END IF;
      END LOOP;

      --wait jobs to finish
      while(true)
      loop
        select count(*)
        into v_job_count
        from lx_e92_worker
        where status = 'RUNNING';

        if(v_job_count > 0) then
          dbms_lock.sleep(3);
        else
          exit;
        end if;
      end loop;

      EXCEPTION
      WHEN OTHERS THEN
      write_log(p_message => SQLERRM);
    END;
  PROCEDURE run_interval
    (
      p_start_index NUMBER,
      p_end_index   NUMBER
    )
  IS
    BEGIN
      INSERT
      INTO lx_E92EO_fixer
      (
        id,
        creditor_id,
        meta_id,
        entity_id,
        key_string
      )
        SELECT t.id,
          t.creditor_id,
          t.meta_id,
          t.entity_id,
          t.key_string
        FROM eav_optimizer t
        WHERE NOT EXISTS
        (SELECT 1 FROM eav_be_entities WHERE id = t.entity_id
        )
              AND t.id >= p_start_index
              AND t.id  < p_end_index;
      UPDATE lx_E92_worker
      SET status     = 'COMPLETED',
        end_date     = systimestamp
      WHERE start_id = p_start_index
            AND end_id     = p_end_index;
      COMMIT;

      EXCEPTION
      WHEN OTHERS THEN
      write_log(p_message => SQLERRM);
    END;
END PKG_E92EO_FIX;
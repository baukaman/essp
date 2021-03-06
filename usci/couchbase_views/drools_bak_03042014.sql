toc.dat                                                                                             100600  004000  002000  00000023644 12317201443 007310  0                                                                                                    ustar00                                                                                                                                                                                                                                                        PGDMP       /    +                r            drools    9.2.2    9.2.2 (    �           0    0    ENCODING    ENCODING        SET client_encoding = 'UTF8';
                       false         �           0    0 
   STDSTRINGS 
   STDSTRINGS     (   SET standard_conforming_strings = 'on';
                       false         �           1262    16453    drools    DATABASE     �   CREATE DATABASE drools WITH TEMPLATE = template0 ENCODING = 'UTF8' LC_COLLATE = 'English_United States.1252' LC_CTYPE = 'English_United States.1252';
    DROP DATABASE drools;
             postgres    false                     2615    2200    public    SCHEMA        CREATE SCHEMA public;
    DROP SCHEMA public;
             postgres    false         �           0    0    SCHEMA public    COMMENT     6   COMMENT ON SCHEMA public IS 'standard public schema';
                  postgres    false    6         �           0    0    public    ACL     �   REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;
                  postgres    false    6         �            3079    11727    plpgsql 	   EXTENSION     ?   CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;
    DROP EXTENSION plpgsql;
                  false         �           0    0    EXTENSION plpgsql    COMMENT     @   COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';
                       false    176         �            1259    16454    package_versions    TABLE     v   CREATE TABLE package_versions (
    repdate date NOT NULL,
    package_id bigint NOT NULL,
    id integer NOT NULL
);
 $   DROP TABLE public.package_versions;
       public         postgres    false    6         �            1259    16457    package_versions_id_seq    SEQUENCE     y   CREATE SEQUENCE package_versions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
 .   DROP SEQUENCE public.package_versions_id_seq;
       public       postgres    false    6    168         �           0    0    package_versions_id_seq    SEQUENCE OWNED BY     E   ALTER SEQUENCE package_versions_id_seq OWNED BY package_versions.id;
            public       postgres    false    169         �            1259    16459    packages    TABLE     �   CREATE TABLE packages (
    name character varying,
    repdate date,
    id integer NOT NULL,
    description character varying
);
    DROP TABLE public.packages;
       public         postgres    false    6         �            1259    16465    packages_id_seq    SEQUENCE     q   CREATE SEQUENCE packages_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
 &   DROP SEQUENCE public.packages_id_seq;
       public       postgres    false    6    170         �           0    0    packages_id_seq    SEQUENCE OWNED BY     5   ALTER SEQUENCE packages_id_seq OWNED BY packages.id;
            public       postgres    false    171         �            1259    16467    rule_package_versions    TABLE     �   CREATE TABLE rule_package_versions (
    rule_id bigint NOT NULL,
    package_versions_id bigint NOT NULL,
    id integer NOT NULL
);
 )   DROP TABLE public.rule_package_versions;
       public         postgres    false    6         �            1259    16470    rule_package_versions_id_seq    SEQUENCE     ~   CREATE SEQUENCE rule_package_versions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
 3   DROP SEQUENCE public.rule_package_versions_id_seq;
       public       postgres    false    172    6         �           0    0    rule_package_versions_id_seq    SEQUENCE OWNED BY     O   ALTER SEQUENCE rule_package_versions_id_seq OWNED BY rule_package_versions.id;
            public       postgres    false    173         �            1259    16472    rules    TABLE     i   CREATE TABLE rules (
    id integer NOT NULL,
    rule character varying,
    title character varying
);
    DROP TABLE public.rules;
       public         postgres    false    6         �            1259    16478    rules_id_seq    SEQUENCE     n   CREATE SEQUENCE rules_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
 #   DROP SEQUENCE public.rules_id_seq;
       public       postgres    false    174    6         �           0    0    rules_id_seq    SEQUENCE OWNED BY     /   ALTER SEQUENCE rules_id_seq OWNED BY rules.id;
            public       postgres    false    175         �           2604    16480    id    DEFAULT     l   ALTER TABLE ONLY package_versions ALTER COLUMN id SET DEFAULT nextval('package_versions_id_seq'::regclass);
 B   ALTER TABLE public.package_versions ALTER COLUMN id DROP DEFAULT;
       public       postgres    false    169    168         �           2604    16481    id    DEFAULT     \   ALTER TABLE ONLY packages ALTER COLUMN id SET DEFAULT nextval('packages_id_seq'::regclass);
 :   ALTER TABLE public.packages ALTER COLUMN id DROP DEFAULT;
       public       postgres    false    171    170         �           2604    16482    id    DEFAULT     v   ALTER TABLE ONLY rule_package_versions ALTER COLUMN id SET DEFAULT nextval('rule_package_versions_id_seq'::regclass);
 G   ALTER TABLE public.rule_package_versions ALTER COLUMN id DROP DEFAULT;
       public       postgres    false    173    172         �           2604    16483    id    DEFAULT     V   ALTER TABLE ONLY rules ALTER COLUMN id SET DEFAULT nextval('rules_id_seq'::regclass);
 7   ALTER TABLE public.rules ALTER COLUMN id DROP DEFAULT;
       public       postgres    false    175    174         �          0    16454    package_versions 
   TABLE DATA               <   COPY package_versions (repdate, package_id, id) FROM stdin;
    public       postgres    false    168       1953.dat �           0    0    package_versions_id_seq    SEQUENCE SET     >   SELECT pg_catalog.setval('package_versions_id_seq', 7, true);
            public       postgres    false    169         �          0    16459    packages 
   TABLE DATA               ;   COPY packages (name, repdate, id, description) FROM stdin;
    public       postgres    false    170       1955.dat �           0    0    packages_id_seq    SEQUENCE SET     7   SELECT pg_catalog.setval('packages_id_seq', 16, true);
            public       postgres    false    171         �          0    16467    rule_package_versions 
   TABLE DATA               J   COPY rule_package_versions (rule_id, package_versions_id, id) FROM stdin;
    public       postgres    false    172       1957.dat �           0    0    rule_package_versions_id_seq    SEQUENCE SET     D   SELECT pg_catalog.setval('rule_package_versions_id_seq', 12, true);
            public       postgres    false    173         �          0    16472    rules 
   TABLE DATA               )   COPY rules (id, rule, title) FROM stdin;
    public       postgres    false    174       1959.dat �           0    0    rules_id_seq    SEQUENCE SET     3   SELECT pg_catalog.setval('rules_id_seq', 3, true);
            public       postgres    false    175         �           2606    16485    pk 
   CONSTRAINT     O   ALTER TABLE ONLY rule_package_versions
    ADD CONSTRAINT pk PRIMARY KEY (id);
 B   ALTER TABLE ONLY public.rule_package_versions DROP CONSTRAINT pk;
       public         postgres    false    172    172         �           2606    16487    pk_package_versions 
   CONSTRAINT     [   ALTER TABLE ONLY package_versions
    ADD CONSTRAINT pk_package_versions PRIMARY KEY (id);
 N   ALTER TABLE ONLY public.package_versions DROP CONSTRAINT pk_package_versions;
       public         postgres    false    168    168         �           2606    16489    pk_packages 
   CONSTRAINT     K   ALTER TABLE ONLY packages
    ADD CONSTRAINT pk_packages PRIMARY KEY (id);
 >   ALTER TABLE ONLY public.packages DROP CONSTRAINT pk_packages;
       public         postgres    false    170    170         �           2606    16491 
   rules_pkey 
   CONSTRAINT     G   ALTER TABLE ONLY rules
    ADD CONSTRAINT rules_pkey PRIMARY KEY (id);
 :   ALTER TABLE ONLY public.rules DROP CONSTRAINT rules_pkey;
       public         postgres    false    174    174         �           2606    16493    unique_name 
   CONSTRAINT     H   ALTER TABLE ONLY packages
    ADD CONSTRAINT unique_name UNIQUE (name);
 >   ALTER TABLE ONLY public.packages DROP CONSTRAINT unique_name;
       public         postgres    false    170    170         �           2606    16494    fk_package_versions    FK CONSTRAINT     �   ALTER TABLE ONLY rule_package_versions
    ADD CONSTRAINT fk_package_versions FOREIGN KEY (package_versions_id) REFERENCES package_versions(id);
 S   ALTER TABLE ONLY public.rule_package_versions DROP CONSTRAINT fk_package_versions;
       public       postgres    false    1941    172    168         �           2606    16499    fk_packages    FK CONSTRAINT     s   ALTER TABLE ONLY package_versions
    ADD CONSTRAINT fk_packages FOREIGN KEY (package_id) REFERENCES packages(id);
 F   ALTER TABLE ONLY public.package_versions DROP CONSTRAINT fk_packages;
       public       postgres    false    168    1943    170         �           2606    16504    fk_rule    FK CONSTRAINT     n   ALTER TABLE ONLY rule_package_versions
    ADD CONSTRAINT fk_rule FOREIGN KEY (rule_id) REFERENCES rules(id);
 G   ALTER TABLE ONLY public.rule_package_versions DROP CONSTRAINT fk_rule;
       public       postgres    false    1949    174    172                                                                                                    1953.dat                                                                                            100600  004000  002000  00000000156 12317201443 007115  0                                                                                                    ustar00                                                                                                                                                                                                                                                        2013-09-18	1	1
2013-09-19	2	2
2013-10-22	1	3
2013-10-22	2	4
2013-10-23	2	5
2013-10-28	3	6
2014-02-26	4	7
\.


                                                                                                                                                                                                                                                                                                                                                                                                                  1955.dat                                                                                            100600  004000  002000  00000000166 12317201443 007120  0                                                                                                    ustar00                                                                                                                                                                                                                                                        test	2013-09-18	1	\N
ct_package_parser	2013-09-18	2	\N
test_parser	2013-10-28	3	\N
credit_parser	2014-02-26	4	\N
\.


                                                                                                                                                                                                                                                                                                                                                                                                          1957.dat                                                                                            100600  004000  002000  00000000005 12317201443 007112  0                                                                                                    ustar00                                                                                                                                                                                                                                                        \.


                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           1959.dat                                                                                            100600  004000  002000  00000001773 12317201443 007131  0                                                                                                    ustar00                                                                                                                                                                                                                                                        3	rule "i_test"\r\n    when\r\n        $entity : BaseEntity (getEl("a_integer") == null)\r\n    then\r\n        $entity.addValidationError("Число А обязательный параметр");\r\nend	\N
1	rule "RNN_test3"\r\n    when\r\n        $entity : BaseEntity ( \r\n           getEl("subjects.person.docs[doc_type->code=11].no") != null && \r\n           !BRMSHelper.isValidRNN((String)getEl("subjects.person.docs[doc_type->code=11].no")) )\r\n    then\r\n        $entity.addValidationError("РНН не проходит проверку контрольной суммы");\r\nend	\N
2	rule "date_test"\n    when\n        $entity : BaseEntity ( \n           getEl("credit.actual_issue_date") != null && getEl("credit.contract_maturity_date") != null && ((Date)getEl("credit.actual_issue_date")).compareTo((Date)getEl("credit.contract_maturity_date")) > 0  )\n    then\n        $entity.addValidationError("Дата выдачи договора меньше даты закрытия");\nend	date_test
\.


     restore.sql                                                                                         100600  004000  002000  00000021232 12317201443 010224  0                                                                                                    ustar00                                                                                                                                                                                                                                                        --
-- NOTE:
--
-- File paths need to be edited. Search for $$PATH$$ and
-- replace it with the path to the directory containing
-- the extracted data files.
--
--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

SET search_path = public, pg_catalog;

ALTER TABLE ONLY public.rule_package_versions DROP CONSTRAINT fk_rule;
ALTER TABLE ONLY public.package_versions DROP CONSTRAINT fk_packages;
ALTER TABLE ONLY public.rule_package_versions DROP CONSTRAINT fk_package_versions;
ALTER TABLE ONLY public.packages DROP CONSTRAINT unique_name;
ALTER TABLE ONLY public.rules DROP CONSTRAINT rules_pkey;
ALTER TABLE ONLY public.packages DROP CONSTRAINT pk_packages;
ALTER TABLE ONLY public.package_versions DROP CONSTRAINT pk_package_versions;
ALTER TABLE ONLY public.rule_package_versions DROP CONSTRAINT pk;
ALTER TABLE public.rules ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.rule_package_versions ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.packages ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.package_versions ALTER COLUMN id DROP DEFAULT;
DROP SEQUENCE public.rules_id_seq;
DROP TABLE public.rules;
DROP SEQUENCE public.rule_package_versions_id_seq;
DROP TABLE public.rule_package_versions;
DROP SEQUENCE public.packages_id_seq;
DROP TABLE public.packages;
DROP SEQUENCE public.package_versions_id_seq;
DROP TABLE public.package_versions;
DROP EXTENSION plpgsql;
DROP SCHEMA public;
--
-- Name: public; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA public;


ALTER SCHEMA public OWNER TO postgres;

--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: postgres
--

COMMENT ON SCHEMA public IS 'standard public schema';


--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: package_versions; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE package_versions (
    repdate date NOT NULL,
    package_id bigint NOT NULL,
    id integer NOT NULL
);


ALTER TABLE public.package_versions OWNER TO postgres;

--
-- Name: package_versions_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE package_versions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.package_versions_id_seq OWNER TO postgres;

--
-- Name: package_versions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE package_versions_id_seq OWNED BY package_versions.id;


--
-- Name: packages; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE packages (
    name character varying,
    repdate date,
    id integer NOT NULL,
    description character varying
);


ALTER TABLE public.packages OWNER TO postgres;

--
-- Name: packages_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE packages_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.packages_id_seq OWNER TO postgres;

--
-- Name: packages_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE packages_id_seq OWNED BY packages.id;


--
-- Name: rule_package_versions; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE rule_package_versions (
    rule_id bigint NOT NULL,
    package_versions_id bigint NOT NULL,
    id integer NOT NULL
);


ALTER TABLE public.rule_package_versions OWNER TO postgres;

--
-- Name: rule_package_versions_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE rule_package_versions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.rule_package_versions_id_seq OWNER TO postgres;

--
-- Name: rule_package_versions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE rule_package_versions_id_seq OWNED BY rule_package_versions.id;


--
-- Name: rules; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE rules (
    id integer NOT NULL,
    rule character varying,
    title character varying
);


ALTER TABLE public.rules OWNER TO postgres;

--
-- Name: rules_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE rules_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.rules_id_seq OWNER TO postgres;

--
-- Name: rules_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE rules_id_seq OWNED BY rules.id;


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY package_versions ALTER COLUMN id SET DEFAULT nextval('package_versions_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY packages ALTER COLUMN id SET DEFAULT nextval('packages_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY rule_package_versions ALTER COLUMN id SET DEFAULT nextval('rule_package_versions_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY rules ALTER COLUMN id SET DEFAULT nextval('rules_id_seq'::regclass);


--
-- Data for Name: package_versions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY package_versions (repdate, package_id, id) FROM stdin;
\.
COPY package_versions (repdate, package_id, id) FROM '$$PATH$$/1953.dat';

--
-- Name: package_versions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('package_versions_id_seq', 7, true);


--
-- Data for Name: packages; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY packages (name, repdate, id, description) FROM stdin;
\.
COPY packages (name, repdate, id, description) FROM '$$PATH$$/1955.dat';

--
-- Name: packages_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('packages_id_seq', 16, true);


--
-- Data for Name: rule_package_versions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY rule_package_versions (rule_id, package_versions_id, id) FROM stdin;
\.
COPY rule_package_versions (rule_id, package_versions_id, id) FROM '$$PATH$$/1957.dat';

--
-- Name: rule_package_versions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('rule_package_versions_id_seq', 12, true);


--
-- Data for Name: rules; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY rules (id, rule, title) FROM stdin;
\.
COPY rules (id, rule, title) FROM '$$PATH$$/1959.dat';

--
-- Name: rules_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('rules_id_seq', 3, true);


--
-- Name: pk; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY rule_package_versions
    ADD CONSTRAINT pk PRIMARY KEY (id);


--
-- Name: pk_package_versions; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY package_versions
    ADD CONSTRAINT pk_package_versions PRIMARY KEY (id);


--
-- Name: pk_packages; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY packages
    ADD CONSTRAINT pk_packages PRIMARY KEY (id);


--
-- Name: rules_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY rules
    ADD CONSTRAINT rules_pkey PRIMARY KEY (id);


--
-- Name: unique_name; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY packages
    ADD CONSTRAINT unique_name UNIQUE (name);


--
-- Name: fk_package_versions; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY rule_package_versions
    ADD CONSTRAINT fk_package_versions FOREIGN KEY (package_versions_id) REFERENCES package_versions(id);


--
-- Name: fk_packages; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY package_versions
    ADD CONSTRAINT fk_packages FOREIGN KEY (package_id) REFERENCES packages(id);


--
-- Name: fk_rule; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY rule_package_versions
    ADD CONSTRAINT fk_rule FOREIGN KEY (rule_id) REFERENCES rules(id);


--
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      
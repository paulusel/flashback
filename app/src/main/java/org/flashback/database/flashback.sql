--
-- PostgreSQL database dump
--

-- Dumped from database version 17.4
-- Dumped by pg_dump version 17.4

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: flashback; Type: SCHEMA; Schema: -; Owner: flashback
--

CREATE SCHEMA flashback;


ALTER SCHEMA flashback OWNER TO flashback;

--
-- Name: update_modified(); Type: FUNCTION; Schema: flashback; Owner: flashback
--

CREATE FUNCTION flashback.update_modified() RETURNS trigger
    LANGUAGE plpgsql
    AS $$ begin NEW.modified = now(); return NEW; end; $$;


ALTER FUNCTION flashback.update_modified() OWNER TO flashback;

--
-- Name: update_modified_time(); Type: FUNCTION; Schema: flashback; Owner: flashback
--

CREATE FUNCTION flashback.update_modified_time() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
new.modified = current_timestamp;
return new;
end;
$$;


ALTER FUNCTION flashback.update_modified_time() OWNER TO flashback;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: files; Type: TABLE; Schema: flashback; Owner: flashback
--

CREATE TABLE flashback.files (
    file_id text NOT NULL,
    extension text NOT NULL,
    mime_type text NOT NULL,
    size bigint NOT NULL,
    telegram_file_id text
);


ALTER TABLE flashback.files OWNER TO flashback;

--
-- Name: note_files; Type: TABLE; Schema: flashback; Owner: flashback
--

CREATE TABLE flashback.note_files (
    note_id integer NOT NULL,
    file_id text NOT NULL
);


ALTER TABLE flashback.note_files OWNER TO flashback;

--
-- Name: note_tags; Type: TABLE; Schema: flashback; Owner: flashback
--

CREATE TABLE flashback.note_tags (
    note_id integer NOT NULL,
    tag text NOT NULL
);


ALTER TABLE flashback.note_tags OWNER TO flashback;

--
-- Name: notes; Type: TABLE; Schema: flashback; Owner: flashback
--

CREATE TABLE flashback.notes (
    note_id integer NOT NULL,
    note text,
    modified timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    created timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    owner_id integer NOT NULL
);


ALTER TABLE flashback.notes OWNER TO flashback;

--
-- Name: notes_note_id_seq; Type: SEQUENCE; Schema: flashback; Owner: flashback
--

CREATE SEQUENCE flashback.notes_note_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE flashback.notes_note_id_seq OWNER TO flashback;

--
-- Name: notes_note_id_seq; Type: SEQUENCE OWNED BY; Schema: flashback; Owner: flashback
--

ALTER SEQUENCE flashback.notes_note_id_seq OWNED BY flashback.notes.note_id;


--
-- Name: users; Type: TABLE; Schema: flashback; Owner: flashback
--

CREATE TABLE flashback.users (
    user_id integer NOT NULL,
    username text NOT NULL,
    password text NOT NULL,
    telegram_chat_id bigint,
    telegram_user_id bigint
);


ALTER TABLE flashback.users OWNER TO flashback;

--
-- Name: users_user_id_seq; Type: SEQUENCE; Schema: flashback; Owner: flashback
--

CREATE SEQUENCE flashback.users_user_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE flashback.users_user_id_seq OWNER TO flashback;

--
-- Name: users_user_id_seq; Type: SEQUENCE OWNED BY; Schema: flashback; Owner: flashback
--

ALTER SEQUENCE flashback.users_user_id_seq OWNED BY flashback.users.user_id;


--
-- Name: notes note_id; Type: DEFAULT; Schema: flashback; Owner: flashback
--

ALTER TABLE ONLY flashback.notes ALTER COLUMN note_id SET DEFAULT nextval('flashback.notes_note_id_seq'::regclass);


--
-- Name: users user_id; Type: DEFAULT; Schema: flashback; Owner: flashback
--

ALTER TABLE ONLY flashback.users ALTER COLUMN user_id SET DEFAULT nextval('flashback.users_user_id_seq'::regclass);


--
-- Data for Name: files; Type: TABLE DATA; Schema: flashback; Owner: flashback
--

COPY flashback.files (file_id, extension, mime_type, size, telegram_file_id) FROM stdin;
\.


--
-- Data for Name: note_files; Type: TABLE DATA; Schema: flashback; Owner: flashback
--

COPY flashback.note_files (note_id, file_id) FROM stdin;
\.


--
-- Data for Name: note_tags; Type: TABLE DATA; Schema: flashback; Owner: flashback
--

COPY flashback.note_tags (note_id, tag) FROM stdin;
\.


--
-- Data for Name: notes; Type: TABLE DATA; Schema: flashback; Owner: flashback
--

COPY flashback.notes (note_id, note, modified, created, owner_id) FROM stdin;
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: flashback; Owner: flashback
--

COPY flashback.users (user_id, username, password, telegram_chat_id, telegram_user_id) FROM stdin;
\.


--
-- Name: notes_note_id_seq; Type: SEQUENCE SET; Schema: flashback; Owner: flashback
--

SELECT pg_catalog.setval('flashback.notes_note_id_seq', 1, false);


--
-- Name: users_user_id_seq; Type: SEQUENCE SET; Schema: flashback; Owner: flashback
--

SELECT pg_catalog.setval('flashback.users_user_id_seq', 1, false);


--
-- Name: files files_pkey; Type: CONSTRAINT; Schema: flashback; Owner: flashback
--

ALTER TABLE ONLY flashback.files
    ADD CONSTRAINT files_pkey PRIMARY KEY (file_id);


--
-- Name: note_files note_files_pkey; Type: CONSTRAINT; Schema: flashback; Owner: flashback
--

ALTER TABLE ONLY flashback.note_files
    ADD CONSTRAINT note_files_pkey PRIMARY KEY (note_id, file_id);


--
-- Name: note_tags note_tags_pkey; Type: CONSTRAINT; Schema: flashback; Owner: flashback
--

ALTER TABLE ONLY flashback.note_tags
    ADD CONSTRAINT note_tags_pkey PRIMARY KEY (note_id, tag);


--
-- Name: notes notes_pkey; Type: CONSTRAINT; Schema: flashback; Owner: flashback
--

ALTER TABLE ONLY flashback.notes
    ADD CONSTRAINT notes_pkey PRIMARY KEY (note_id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: flashback; Owner: flashback
--

ALTER TABLE ONLY flashback.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (user_id);


--
-- Name: notes trigger_update_note_modified; Type: TRIGGER; Schema: flashback; Owner: flashback
--

CREATE TRIGGER trigger_update_note_modified BEFORE UPDATE ON flashback.notes FOR EACH ROW EXECUTE FUNCTION flashback.update_modified();


--
-- Name: note_files note_files_file_id_fkey; Type: FK CONSTRAINT; Schema: flashback; Owner: flashback
--

ALTER TABLE ONLY flashback.note_files
    ADD CONSTRAINT note_files_file_id_fkey FOREIGN KEY (file_id) REFERENCES flashback.files(file_id) ON DELETE RESTRICT;


--
-- Name: note_files note_files_note_id_fkey; Type: FK CONSTRAINT; Schema: flashback; Owner: flashback
--

ALTER TABLE ONLY flashback.note_files
    ADD CONSTRAINT note_files_note_id_fkey FOREIGN KEY (note_id) REFERENCES flashback.notes(note_id) ON DELETE CASCADE;


--
-- Name: note_tags note_tags_note_id_fkey; Type: FK CONSTRAINT; Schema: flashback; Owner: flashback
--

ALTER TABLE ONLY flashback.note_tags
    ADD CONSTRAINT note_tags_note_id_fkey FOREIGN KEY (note_id) REFERENCES flashback.notes(note_id) ON DELETE CASCADE;


--
-- Name: notes notes_owner_id_fkey; Type: FK CONSTRAINT; Schema: flashback; Owner: flashback
--

ALTER TABLE ONLY flashback.notes
    ADD CONSTRAINT notes_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES flashback.users(user_id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--


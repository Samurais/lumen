CREATE TABLE lumen.yagoentity (
    id serial PRIMARY KEY,
    nn varchar(4000) NOT NULL,
    preflabel varchar(4000),
    ispreferredmeaningof varchar(4000),
    hasgivenname varchar(255),
    hasfamilyname varchar(255),
    hasgloss text,
    redirectedfrom varchar(4000),
    CONSTRAINT uk_yagoentity_nn UNIQUE (nn),
    CONSTRAINT uk_yagoentity_ispreferredmeaningof UNIQUE (ispreferredmeaningof)
);
CREATE INDEX ik_yagoentity_preflabel ON lumen.yagoentity (preflabel);
CREATE INDEX ik_yagoentity_redirectedfrom ON lumen.yagoentity (redirectedfrom);

CREATE TABLE lumen.yagolabel (
    id serial PRIMARY KEY,
    entity_id int NOT NULL REFERENCES lumen.yagoentity,
    inlanguage varchar(255),
    value varchar(4000)
);
CREATE INDEX ik_yagolabel_entity_id ON lumen.yagolabel (entity_id);
CREATE INDEX ik_yagolabel_inlanguage ON lumen.yagolabel (inlanguage);

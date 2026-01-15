-- Clean table before inserting test data
DELETE FROM elstar_daten;

-- Reset identity counter for H2
ALTER TABLE elstar_daten ALTER COLUMN id RESTART WITH 1;

-- Test data for ElstarDaten entity
INSERT INTO elstar_daten (id, xml_nachricht) VALUES (1, '<ElstarDaten><PersonalNr>12345</PersonalNr><Steuerklasse>1</Steuerklasse></ElstarDaten>');
INSERT INTO elstar_daten (id, xml_nachricht) VALUES (2, '<ElstarDaten><PersonalNr>67890</PersonalNr><Steuerklasse>3</Steuerklasse></ElstarDaten>');
INSERT INTO elstar_daten (id, xml_nachricht) VALUES (3, '<ElstarDaten><PersonalNr>11111</PersonalNr><Steuerklasse>4</Steuerklasse></ElstarDaten>');
INSERT INTO elstar_daten (id, xml_nachricht) VALUES (4, '<ElstarDaten><PersonalNr>22222</PersonalNr><Steuerklasse>5</Steuerklasse></ElstarDaten>');
INSERT INTO elstar_daten (id, xml_nachricht) VALUES (5, '<ElstarDaten><PersonalNr>33333</PersonalNr><Steuerklasse>6</Steuerklasse></ElstarDaten>');
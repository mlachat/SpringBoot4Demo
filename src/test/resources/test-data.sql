-- Clean table before inserting test data
DELETE FROM elstar_daten;

-- Reset identity counter for H2
ALTER TABLE elstar_daten ALTER COLUMN id RESTART WITH 1;

-- Test data for ElstarDaten entity
INSERT INTO elstar_daten (id, xml_nachricht, creation_date, status) VALUES (1, '<ElstarDaten><PersonalNr>12345</PersonalNr><Steuerklasse>1</Steuerklasse></ElstarDaten>', '2025-01-10', 0);
INSERT INTO elstar_daten (id, xml_nachricht, creation_date, status) VALUES (2, '<ElstarDaten><PersonalNr>67890</PersonalNr><Steuerklasse>3</Steuerklasse></ElstarDaten>', '2025-01-11', 1);
INSERT INTO elstar_daten (id, xml_nachricht, creation_date, status) VALUES (3, '<ElstarDaten><PersonalNr>11111</PersonalNr><Steuerklasse>4</Steuerklasse></ElstarDaten>', '2025-01-12', 0);
INSERT INTO elstar_daten (id, xml_nachricht, creation_date, status) VALUES (4, '<ElstarDaten><PersonalNr>22222</PersonalNr><Steuerklasse>5</Steuerklasse></ElstarDaten>', '2025-01-13', 2);
INSERT INTO elstar_daten (id, xml_nachricht, creation_date, status) VALUES (5, '<ElstarDaten><PersonalNr>33333</PersonalNr><Steuerklasse>6</Steuerklasse></ElstarDaten>', '2025-01-14', 1);
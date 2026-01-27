-- Clean table before inserting test data
DELETE FROM elstar_daten;

-- Reset identity counter for H2
ALTER TABLE elstar_daten ALTER COLUMN id RESTART WITH 1;

-- Test data for ElstarDaten entity (status 0 = pending, will be updated to 1 = processed by receive batch)
INSERT INTO elstar_daten (id, uuid, xml_nachricht, creation_date, status) VALUES (1, 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', '<ElstarDaten><PersonalNr>12345</PersonalNr><Steuerklasse>1</Steuerklasse></ElstarDaten>', '2025-01-10', 0);
INSERT INTO elstar_daten (id, uuid, xml_nachricht, creation_date, status) VALUES (2, 'b2c3d4e5-f6a7-8901-bcde-f12345678901', '<ElstarDaten><PersonalNr>67890</PersonalNr><Steuerklasse>3</Steuerklasse></ElstarDaten>', '2025-01-11', 0);
INSERT INTO elstar_daten (id, uuid, xml_nachricht, creation_date, status) VALUES (3, 'c3d4e5f6-a7b8-9012-cdef-123456789012', '<ElstarDaten><PersonalNr>11111</PersonalNr><Steuerklasse>4</Steuerklasse></ElstarDaten>', '2025-01-12', 0);
INSERT INTO elstar_daten (id, uuid, xml_nachricht, creation_date, status) VALUES (4, 'd4e5f6a7-b8c9-0123-def0-234567890123', '<ElstarDaten><PersonalNr>22222</PersonalNr><Steuerklasse>5</Steuerklasse></ElstarDaten>', '2025-01-13', 0);
INSERT INTO elstar_daten (id, uuid, xml_nachricht, creation_date, status) VALUES (5, 'e5f6a7b8-c9d0-1234-ef01-345678901234', '<ElstarDaten><PersonalNr>33333</PersonalNr><Steuerklasse>6</Steuerklasse></ElstarDaten>', '2025-01-14', 0);

-- Reset identity counter to continue after inserted data
ALTER TABLE elstar_daten ALTER COLUMN id RESTART WITH 6;
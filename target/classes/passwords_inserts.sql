BEGIN TRANSACTION;

-- Admin password: admin
INSERT INTO user_passwords (email, password_hash, salt, role, level) 
VALUES ('admin@healthassistant.com', 'FPfhP+vAdE0C1ELnAmpeGtLal/YlRUzUfzAxQ1wzXCM=', '/q5iBEp+7Uy3bimcsciFPQ==', 'admin', 3);

-- Root password: root
INSERT INTO user_passwords (email, password_hash, salt, role, level) 
VALUES ('root@healthassistant.com', 'ewCK83HlE9NAtwWKv+eK9Y5pED9SacTSwLLa5CfATUQ=', 'NFOSKbuPsojGHyGrfPFqqA==', 'admin', 3);

-- Default doctor user connected to an existing doctor record
INSERT INTO user_passwords (email, password_hash, salt, role, level, patient_id)
VALUES ('dr.walsh@healthassistant.com', 'doctor', NULL, 'doctor', 2, NULL);

-- Default patient user connected to an existing patient record
INSERT INTO user_passwords (email, password_hash, salt, role, level, patient_id)
VALUES ('sebastian.thompson77@hotmail.co.uk', 'patient', NULL, 'patient', 1, 'pt-0017');

COMMIT;


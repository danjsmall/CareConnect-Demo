BEGIN TRANSACTION;

INSERT INTO administrators (email, name) VALUES ('admin@healthassistant.com', 'System Admin');
INSERT INTO administrators (email, name) VALUES ('root@healthassistant.com', 'Root');

COMMIT;

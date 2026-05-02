INSERT INTO `user` (nom, prenom, email, password, roles, type) VALUES ('Admin', 'Super', 'admin@smartpath.tn', 'admin', '["ROLE_ADMIN"]', 'admin');
INSERT INTO `user` (nom, prenom, email, password, roles, type) VALUES ('Prof', 'Test', 'prof@smartpath.tn', 'prof', '["ROLE_PROF"]', 'prof');
-- Let's fetch the ID of the prof just inserted to link in prof table
INSERT INTO prof (id, specialite) SELECT id, 'Informatique' FROM `user` WHERE email = 'prof@smartpath.tn';

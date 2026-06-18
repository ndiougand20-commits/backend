-- Seed complete REZO dataset for frontend testing
-- Usage:
--   psql -U rezo_user -d rezo_db -f docs/seed_front_test_data.sql

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Reset all data (safe for local test environment)
TRUNCATE TABLE
  chat_support,
  profile_swipes,
  swipes,
  messages,
  user_media_files,
  profile_competences,
  profile_preferences_secteur,
  profile_preferences_lieu,
  profile_experiences,
  profile_centres_interet,
  school_domaines,
  school_diplomes,
  offer_competences,
  offers,
  profiles,
  companies,
  schools,
  refresh_tokens,
  users,
  packs
CASCADE;

-- Password policy for seeded users
-- Plain password for all test users: password
-- Hash generated at insert time via pgcrypto: crypt('password', gen_salt('bf', 10))

-- Packs
INSERT INTO packs (id, nom, description, prix, cible, features, created_at) VALUES
('10000000-0000-0000-0000-000000000001', 'FREE', 'Pack gratuit de demarrage', 0.00, 'TOUS', 'MATCHING_BASIC', NOW()),
('10000000-0000-0000-0000-000000000002', 'Vision', 'Pack lyceen premium', 5000.00, 'LYCEEN', 'MATCHING_BASIC,MATCHING_PREMIUM', NOW()),
('10000000-0000-0000-0000-000000000003', 'Essentiel', 'Pack candidat essentiel', 5000.00, 'ETUDIANT', 'MATCHING_BASIC', NOW()),
('10000000-0000-0000-0000-000000000004', 'Connexion', 'Pack candidat avec messagerie limitee', 10000.00, 'ETUDIANT', 'MATCHING_BASIC,MESSAGERIE_LIMITEE', NOW()),
('10000000-0000-0000-0000-000000000005', 'REZO', 'Pack premium candidat', 20000.00, 'ETUDIANT', 'MATCHING_BASIC,MATCHING_PREMIUM,MESSAGERIE_ILLIMITEE,AI_CHAT_ACCESS', NOW()),
('10000000-0000-0000-0000-000000000006', 'Visibilite', 'Pack ecole visibilite', 25000.00, 'ECOLE', 'MATCHING_BASIC,OFFERS_PUBLISH,OFFERS_MANAGE', NOW()),
('10000000-0000-0000-0000-000000000007', 'Marketing', 'Pack ecole marketing', 50000.00, 'ECOLE', 'MATCHING_BASIC,MATCHING_PREMIUM,OFFERS_PUBLISH,OFFERS_MANAGE,MESSAGERIE_ILLIMITEE', NOW()),
('10000000-0000-0000-0000-000000000008', 'Recrutement', 'Pack entreprise recrutement', 35000.00, 'ENTREPRISE', 'MATCHING_BASIC,OFFERS_PUBLISH,OFFERS_MANAGE,MESSAGERIE_LIMITEE', NOW()),
('10000000-0000-0000-0000-000000000009', 'Recrutement Pro', 'Pack entreprise premium', 75000.00, 'ENTREPRISE', 'MATCHING_BASIC,MATCHING_PREMIUM,OFFERS_PUBLISH,OFFERS_MANAGE,MESSAGERIE_ILLIMITEE,AI_CHAT_ACCESS', NOW());

-- Users (all use password: password)
INSERT INTO users (id, email, password_hash, prenom, nom, telephone, role, avatar_url, created_at, pack_id) VALUES
('20000000-0000-0000-0000-000000000001', 'admin@rezo.test', crypt('password', gen_salt('bf', 10)), 'Admin', 'REZO', '770000000', 'ADMIN', 'https://i.pravatar.cc/150?img=1', NOW(), '10000000-0000-0000-0000-000000000001'),
-- Etudiants
('20000000-0000-0000-0000-000000000002', 'etudiant1@rezo.test', crypt('password', gen_salt('bf', 10)), 'Moussa', 'Diop', '771111111', 'ETUDIANT', 'https://i.pravatar.cc/150?img=2', NOW(), '10000000-0000-0000-0000-000000000005'),
('20000000-0000-0000-0000-000000000003', 'etudiant2@rezo.test', crypt('password', gen_salt('bf', 10)), 'Awa', 'Ba', '772222222', 'ETUDIANT', 'https://i.pravatar.cc/150?img=3', NOW(), '10000000-0000-0000-0000-000000000004'),
('20000000-0000-0000-0000-000000000010', 'etudiant3@rezo.test', crypt('password', gen_salt('bf', 10)), 'Assane', 'Ndiaye', '779999999', 'ETUDIANT', 'https://i.pravatar.cc/150?img=10', NOW(), '10000000-0000-0000-0000-000000000003'),
-- Lyceens
('20000000-0000-0000-0000-000000000004', 'lyceen1@rezo.test', crypt('password', gen_salt('bf', 10)), 'Ibra', 'Fall', '773333333', 'LYCEEN', 'https://i.pravatar.cc/150?img=4', NOW(), '10000000-0000-0000-0000-000000000002'),
('20000000-0000-0000-0000-000000000011', 'lyceen2@rezo.test', crypt('password', gen_salt('bf', 10)), 'Marie', 'Seck', '774444444', 'LYCEEN', 'https://i.pravatar.cc/150?img=11', NOW(), '10000000-0000-0000-0000-000000000002'),
-- Entreprises
('20000000-0000-0000-0000-000000000006', 'entreprise1@rezo.test', crypt('password', gen_salt('bf', 10)), 'RH', 'TechCorp', '775555555', 'ENTREPRISE', 'https://i.pravatar.cc/150?img=6', NOW(), '10000000-0000-0000-0000-000000000009'),
('20000000-0000-0000-0000-000000000007', 'entreprise2@rezo.test', crypt('password', gen_salt('bf', 10)), 'RH', 'DataSen', '776666666', 'ENTREPRISE', 'https://i.pravatar.cc/150?img=7', NOW(), '10000000-0000-0000-0000-000000000008'),
('20000000-0000-0000-0000-000000000012', 'entreprise3@rezo.test', crypt('password', gen_salt('bf', 10)), 'RH', 'FinanceHub', '777555555', 'ENTREPRISE', 'https://i.pravatar.cc/150?img=12', NOW(), '10000000-0000-0000-0000-000000000001'),
-- Ecoles
('20000000-0000-0000-0000-000000000008', 'ecole1@rezo.test', crypt('password', gen_salt('bf', 10)), 'Direction', 'SupInfo', '777777777', 'ECOLE', 'https://i.pravatar.cc/150?img=8', NOW(), '10000000-0000-0000-0000-000000000007'),
('20000000-0000-0000-0000-000000000009', 'ecole2@rezo.test', crypt('password', gen_salt('bf', 10)), 'Direction', 'BusinessSchool', '778888888', 'ECOLE', 'https://i.pravatar.cc/150?img=9', NOW(), '10000000-0000-0000-0000-000000000006'),
('20000000-0000-0000-0000-000000000013', 'ecole3@rezo.test', crypt('password', gen_salt('bf', 10)), 'Direction', 'UCAD', '779876543', 'ECOLE', 'https://i.pravatar.cc/150?img=13', NOW(), '10000000-0000-0000-0000-000000000001');

-- Profiles (ETUDIANT, LYCEEN)
INSERT INTO profiles (
  id, user_id, niveau_etude, domaine, objectif,
  classe_actuelle, serie_orientation, objectif_postbac, created_at
) VALUES
('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 'Licence 3', 'Informatique', 'Stage de fin d etudes', NULL, NULL, NULL, NOW()),
('30000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003', 'Master 1', 'Data Science', 'Alternance', NULL, NULL, NULL, NOW()),
('30000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000004', NULL, 'Sciences', NULL, 'Terminale', 'S', 'Ingenierie logicielle', NOW()),
('30000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000010', 'Master 2', 'Genie Logiciel', 'Emploi CDI', NULL, NULL, NULL, NOW()),
('30000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000011', NULL, 'Mathematiques', NULL, 'Premiere', 'S', 'Ecole d ingenieurs', NOW());

INSERT INTO profile_competences (profile_id, competence) VALUES
('30000000-0000-0000-0000-000000000001', 'Java'),
('30000000-0000-0000-0000-000000000001', 'Spring Boot'),
('30000000-0000-0000-0000-000000000001', 'PostgreSQL'),
('30000000-0000-0000-0000-000000000002', 'Python'),
('30000000-0000-0000-0000-000000000002', 'Machine Learning'),
('30000000-0000-0000-0000-000000000004', 'Java'),
('30000000-0000-0000-0000-000000000004', 'Docker'),
('30000000-0000-0000-0000-000000000004', 'Kubernetes'),
('30000000-0000-0000-0000-000000000005', 'Calcul'),
('30000000-0000-0000-0000-000000000005', 'Programmation');

INSERT INTO profile_preferences_secteur (profile_id, secteur) VALUES
('30000000-0000-0000-0000-000000000001', 'Tech'),
('30000000-0000-0000-0000-000000000002', 'Banque'),
('30000000-0000-0000-0000-000000000004', 'Tech'),
('30000000-0000-0000-0000-000000000004', 'Cloud');

INSERT INTO profile_preferences_lieu (profile_id, lieu) VALUES
('30000000-0000-0000-0000-000000000001', 'Dakar'),
('30000000-0000-0000-0000-000000000002', 'Thiès'),
('30000000-0000-0000-0000-000000000004', 'Dakar'),
('30000000-0000-0000-0000-000000000004', 'Thiès'),
('30000000-0000-0000-0000-000000000005', 'Dakar');

INSERT INTO profile_experiences (profile_id, experience) VALUES
('30000000-0000-0000-0000-000000000001', 'Stage backend 6 mois'),
('30000000-0000-0000-0000-000000000002', 'Projet IA universitaire'),
('30000000-0000-0000-0000-000000000004', 'Stage DevOps 3 mois chez TechCorp'),
('30000000-0000-0000-0000-000000000004', 'Freelance Kubernetes');

INSERT INTO profile_centres_interet (profile_id, interet) VALUES
('30000000-0000-0000-0000-000000000003', 'Informatique'),
('30000000-0000-0000-0000-000000000003', 'Mathematiques'),
('30000000-0000-0000-0000-000000000003', 'Robotique');

-- Companies
INSERT INTO companies (
  id, user_id, raison_sociale, secteur_activite, taille, description,
  adresse, site_web, logo_url, created_at
) VALUES
('40000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000006', 'TechCorp Senegal', 'Developpement logiciel', 'PME', 'Entreprise specialisee en solutions SaaS et mobile.', 'Dakar Plateau', 'https://techcorp.sn', 'https://logo.clearbit.com/techcorp.sn', NOW()),
('40000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000007', 'DataSen', 'Data & IA', 'ETI', 'Cabinet data engineering et IA appliquee.', 'Dakar Almadies', 'https://datasen.sn', 'https://logo.clearbit.com/datasen.sn', NOW()),
('40000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000012', 'FinanceHub Senegal', 'Services financiers', 'PME', 'Agence de conseil en finance et gestion.', 'Dakar Plateau', 'https://financehub.sn', 'https://logo.clearbit.com/financehub.sn', NOW());

-- Schools
INSERT INTO schools (
  id, user_id, nom_etablissement, statut, description,
  adresse, site_web, logo_url, created_at
) VALUES
('50000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000008', 'Ecole Superieure d Informatique', 'PRIVE', 'Formation en genie logiciel, cloud, data.', 'Dakar Fann', 'https://supinfo.sn', 'https://logo.clearbit.com/supinfo.sn', NOW()),
('50000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000009', 'Business School Dakar', 'PRIVE', 'Formations management, finance, marketing digital.', 'Dakar Point E', 'https://bsd.sn', 'https://logo.clearbit.com/bsd.sn', NOW()),
('50000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000013', 'Universite Cheikh Anta Diop', 'PUBLIC', 'Universite nationale avec formations generales et techniques.', 'Dakar Ngor', 'https://ucad.sn', 'https://logo.clearbit.com/ucad.sn', NOW());

INSERT INTO school_domaines (school_id, domaine) VALUES
('50000000-0000-0000-0000-000000000001', 'Informatique'),
('50000000-0000-0000-0000-000000000001', 'Data Science'),
('50000000-0000-0000-0000-000000000002', 'Finance'),
('50000000-0000-0000-0000-000000000002', 'Marketing'),
('50000000-0000-0000-0000-000000000003', 'Informatique'),
('50000000-0000-0000-0000-000000000003', 'Sciences Naturelles'),
('50000000-0000-0000-0000-000000000003', 'Lettres');

INSERT INTO school_diplomes (school_id, diplome) VALUES
('50000000-0000-0000-0000-000000000001', 'Licence Informatique'),
('50000000-0000-0000-0000-000000000001', 'Master Data Engineering'),
('50000000-0000-0000-0000-000000000002', 'Licence Gestion'),
('50000000-0000-0000-0000-000000000002', 'Master Finance'),
('50000000-0000-0000-0000-000000000003', 'Licence Informatique'),
('50000000-0000-0000-0000-000000000003', 'Master Informatique'),
('50000000-0000-0000-0000-000000000003', 'Licence Sciences Naturelles'),
('50000000-0000-0000-0000-000000000003', 'Master Sciences Naturelles');

-- Offers (some owned by companies, some by schools)
INSERT INTO offers (
  id, titre, description, type, domaine, location,
  date_publication, date_debut, date_fin, pdf_url, created_at,
  owner_entreprise_id, owner_ecole_id
) VALUES
('60000000-0000-0000-0000-000000000001', 'Stage Backend Java', 'Participation au developpement API Spring Boot.', 'STAGE', 'Informatique', 'Dakar', NOW(), NOW() + INTERVAL '15 days', NOW() + INTERVAL '6 months', '/uploads/offers/60000000-0000-0000-0000-000000000001/fiche-poste.pdf', NOW(), '40000000-0000-0000-0000-000000000001', NULL),
('60000000-0000-0000-0000-000000000002', 'Data Analyst Junior', 'Analyse de donnees et dashboarding BI.', 'EMPLOI', 'Data Science', 'Dakar', NOW(), NOW() + INTERVAL '10 days', NULL, '/uploads/offers/60000000-0000-0000-0000-000000000002/job-description.pdf', NOW(), '40000000-0000-0000-0000-000000000002', NULL),
('60000000-0000-0000-0000-000000000003', 'Formation Cloud & DevOps', 'Programme certifiant sur 4 mois.', 'FORMATION', 'Informatique', 'Dakar', NOW(), NOW() + INTERVAL '20 days', NOW() + INTERVAL '4 months', '/uploads/offers/60000000-0000-0000-0000-000000000003/brochure-cloud.pdf', NOW(), NULL, '50000000-0000-0000-0000-000000000001'),
('60000000-0000-0000-0000-000000000004', 'Formation Finance d Entreprise', 'Cycle intensif en finance et controle de gestion.', 'FORMATION', 'Finance', 'Dakar', NOW(), NOW() + INTERVAL '25 days', NOW() + INTERVAL '3 months', '/uploads/offers/60000000-0000-0000-0000-000000000004/programme-finance.pdf', NOW(), NULL, '50000000-0000-0000-0000-000000000002'),
('60000000-0000-0000-0000-000000000005', 'Stage DevOps', 'Gestion infrastructure et CI/CD avec Docker et Kubernetes.', 'STAGE', 'Informatique', 'Dakar', NOW(), NOW() + INTERVAL '8 days', NOW() + INTERVAL '5 months', '/uploads/offers/60000000-0000-0000-0000-000000000005/devops-stage.pdf', NOW(), '40000000-0000-0000-0000-000000000002', NULL),
('60000000-0000-0000-0000-000000000006', 'Ingenieur IA Senior', 'Recherche et developpement en machine learning et NLP.', 'EMPLOI', 'Data Science', 'Thiès', NOW(), NOW() + INTERVAL '5 days', NULL, '/uploads/offers/60000000-0000-0000-0000-000000000006/ia-engineer.pdf', NOW(), '40000000-0000-0000-0000-000000000001', NULL),
('60000000-0000-0000-0000-000000000007', 'Formation Marketing Digital', 'Masterclass sur les reseaux sociaux et SEO.', 'FORMATION', 'Marketing', 'Dakar', NOW(), NOW() + INTERVAL '30 days', NOW() + INTERVAL '2 months', '/uploads/offers/60000000-0000-0000-0000-000000000007/marketing-digital.pdf', NOW(), NULL, '50000000-0000-0000-0000-000000000002'),
('60000000-0000-0000-0000-000000000008', 'Stage Frontend React', 'Developpement interfaces web modernes avec React et TypeScript.', 'STAGE', 'Informatique', 'Dakar', NOW(), NOW() + INTERVAL '12 days', NOW() + INTERVAL '4 months', NULL, NOW(), '40000000-0000-0000-0000-000000000001', NULL);

INSERT INTO offer_competences (offer_id, competence) VALUES
('60000000-0000-0000-0000-000000000001', 'Java'),
('60000000-0000-0000-0000-000000000001', 'Spring Boot'),
('60000000-0000-0000-0000-000000000001', 'REST API'),
('60000000-0000-0000-0000-000000000002', 'SQL'),
('60000000-0000-0000-0000-000000000002', 'Python'),
('60000000-0000-0000-0000-000000000003', 'Linux'),
('60000000-0000-0000-0000-000000000004', 'Excel avance'),
('60000000-0000-0000-0000-000000000005', 'Docker'),
('60000000-0000-0000-0000-000000000005', 'Kubernetes'),
('60000000-0000-0000-0000-000000000005', 'AWS'),
('60000000-0000-0000-0000-000000000006', 'Python'),
('60000000-0000-0000-0000-000000000006', 'TensorFlow'),
('60000000-0000-0000-0000-000000000006', 'NLP'),
('60000000-0000-0000-0000-000000000007', 'Marketing'),
('60000000-0000-0000-0000-000000000007', 'SEO'),
('60000000-0000-0000-0000-000000000008', 'React'),
('60000000-0000-0000-0000-000000000008', 'TypeScript'),
('60000000-0000-0000-0000-000000000008', 'CSS');

-- Swipes
INSERT INTO swipes (id, user_id, offer_id, action, created_at) VALUES
('70000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000001', 'LIKE', NOW()),
('70000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000002', 'LIKE', NOW()),
('70000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000003', '60000000-0000-0000-0000-000000000002', 'DISLIKE', NOW()),
('70000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000004', '60000000-0000-0000-0000-000000000003', 'LIKE', NOW()),
('70000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000010', '60000000-0000-0000-0000-000000000005', 'LIKE', NOW()),
('70000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000010', '60000000-0000-0000-0000-000000000006', 'LIKE', NOW()),
('70000000-0000-0000-0000-000000000007', '20000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000006', 'DISLIKE', NOW()),
('70000000-0000-0000-0000-000000000008', '20000000-0000-0000-0000-000000000011', '60000000-0000-0000-0000-000000000007', 'LIKE', NOW()),
('70000000-0000-0000-0000-000000000009', '20000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000008', 'LIKE', NOW());

-- Profile swipes (recruteurs -> candidats) pour tester les matches mutuels
INSERT INTO profile_swipes (id, swiper_id, target_user_id, action, created_at) VALUES
('71000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000002', 'LIKE', NOW()),
('71000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000003', 'DISLIKE', NOW()),
('71000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000007', '20000000-0000-0000-0000-000000000002', 'LIKE', NOW()),
('71000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000008', '20000000-0000-0000-0000-000000000004', 'LIKE', NOW()),
('71000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000009', '20000000-0000-0000-0000-000000000004', 'LIKE', NOW()),
('71000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000010', 'LIKE', NOW()),
('71000000-0000-0000-0000-000000000007', '20000000-0000-0000-0000-000000000008', '20000000-0000-0000-0000-000000000011', 'LIKE', NOW()),
('71000000-0000-0000-0000-000000000008', '20000000-0000-0000-0000-000000000012', '20000000-0000-0000-0000-000000000002', 'LIKE', NOW()),
('71000000-0000-0000-0000-000000000009', '20000000-0000-0000-0000-000000000009', '20000000-0000-0000-0000-000000000011', 'DISLIKE', NOW()),
('71000000-0000-0000-0000-000000000010', '20000000-0000-0000-0000-000000000013', '20000000-0000-0000-0000-000000000010', 'LIKE', NOW());

-- Messages
INSERT INTO messages (id, sender_id, receiver_id, offer_id, content, is_read, created_at) VALUES
('80000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000006', '60000000-0000-0000-0000-000000000001', 'Bonjour, je suis interesse par le stage backend.', false, NOW() - INTERVAL '2 days'),
('80000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000001', 'Merci, pouvez-vous partager votre CV ?', true, NOW() - INTERVAL '1 day'),
('80000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000008', NULL, 'Je souhaite des infos sur la formation cloud.', false, NOW() - INTERVAL '5 hours'),
('80000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000008', '20000000-0000-0000-0000-000000000003', NULL, 'Excellent ! Pourriez-vous envoyer vos bulletins scolaires ?', true, NOW() - INTERVAL '3 hours'),
('80000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000010', '20000000-0000-0000-0000-000000000007', '60000000-0000-0000-0000-000000000005', 'Je maitrise DevOps et j aime bien ce role.', false, NOW() - INTERVAL '6 hours'),
('80000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000007', '20000000-0000-0000-0000-000000000010', '60000000-0000-0000-0000-000000000005', 'Super ! Quand pouvez-vous commencer ?', false, NOW() - INTERVAL '4 hours'),
('80000000-0000-0000-0000-000000000007', '20000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000012', '60000000-0000-0000-0000-000000000006', 'Bonjour, je suis tres intéressé par ce poste IA.', false, NOW() - INTERVAL '12 hours'),
('80000000-0000-0000-0000-000000000008', '20000000-0000-0000-0000-000000000011', '20000000-0000-0000-0000-000000000009', '60000000-0000-0000-0000-000000000007', 'Quand commence la formation marketing ?', true, NOW() - INTERVAL '1 day');

-- User media files
INSERT INTO user_media_files (id, user_id, category, original_file_name, content_type, file_url, created_at) VALUES
('90000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 'PHOTO', 'avatar-moussa.jpg', 'image/jpeg', '/uploads/users/20000000-0000-0000-0000-000000000002/photos/avatar-moussa.jpg', NOW()),
('90000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', 'CV', 'cv-moussa.pdf', 'application/pdf', '/uploads/users/20000000-0000-0000-0000-000000000002/justificatifs/cv-moussa.pdf', NOW()),
('90000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000004', 'PHOTO', 'avatar-ibra.png', 'image/png', '/uploads/users/20000000-0000-0000-0000-000000000004/photos/avatar-ibra.png', NOW()),
('90000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000004', 'BULLETIN', 'bulletin-t1.pdf', 'application/pdf', '/uploads/users/20000000-0000-0000-0000-000000000004/justificatifs/bulletin-t1.pdf', NOW()),
('90000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000006', 'JUSTIFICATIF_ENTREPRISE', 'kbis-techcorp.pdf', 'application/pdf', '/uploads/users/20000000-0000-0000-0000-000000000006/justificatifs/kbis-techcorp.pdf', NOW()),
('90000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000010', 'PHOTO', 'avatar-assane.png', 'image/png', '/uploads/users/20000000-0000-0000-0000-000000000010/photos/avatar-assane.png', NOW()),
('90000000-0000-0000-0000-000000000007', '20000000-0000-0000-0000-000000000010', 'CV', 'cv-assane.pdf', 'application/pdf', '/uploads/users/20000000-0000-0000-0000-000000000010/justificatifs/cv-assane.pdf', NOW()),
('90000000-0000-0000-0000-000000000008', '20000000-0000-0000-0000-000000000003', 'PHOTO', 'avatar-awa.jpg', 'image/jpeg', '/uploads/users/20000000-0000-0000-0000-000000000003/photos/avatar-awa.jpg', NOW()),
('90000000-0000-0000-0000-000000000009', '20000000-0000-0000-0000-000000000003', 'CV', 'cv-awa-master.pdf', 'application/pdf', '/uploads/users/20000000-0000-0000-0000-000000000003/justificatifs/cv-awa-master.pdf', NOW()),
('90000000-0000-0000-0000-000000000010', '20000000-0000-0000-0000-000000000007', 'JUSTIFICATIF_ENTREPRISE', 'kbis-datasen.pdf', 'application/pdf', '/uploads/users/20000000-0000-0000-0000-000000000007/justificatifs/kbis-datasen.pdf', NOW()),
('90000000-0000-0000-0000-000000000011', '20000000-0000-0000-0000-000000000008', 'PHOTO', 'logo-supinfo.png', 'image/png', '/uploads/users/20000000-0000-0000-0000-000000000008/photos/logo-supinfo.png', NOW()),
('90000000-0000-0000-0000-000000000012', '20000000-0000-0000-0000-000000000009', 'PHOTO', 'logo-businessschool.png', 'image/png', '/uploads/users/20000000-0000-0000-0000-000000000009/photos/logo-businessschool.png', NOW()),
('90000000-0000-0000-0000-000000000013', '20000000-0000-0000-0000-000000000012', 'JUSTIFICATIF_ENTREPRISE', 'kbis-financehub.pdf', 'application/pdf', '/uploads/users/20000000-0000-0000-0000-000000000012/justificatifs/kbis-financehub.pdf', NOW());

-- Chat support history
INSERT INTO chat_support (id, user_id, user_message, ia_response, context, session_id, created_at) VALUES
('a0000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 'Quel pack me conseillez-vous pour la messagerie illimitee ?', 'Le pack REZO offre messagerie illimitee et chat IA.', 'PACK_RECO', 'b0000000-0000-0000-0000-000000000001', NOW() - INTERVAL '3 days'),
('a0000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000004', 'Je cherche une ecole informatique.', 'Je recommande de consulter les formations cloud et data disponibles.', 'SCHOOL_GUIDANCE', 'b0000000-0000-0000-0000-000000000002', NOW() - INTERVAL '10 hours'),
('a0000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000002', 'Comment modifier mon profil ?', 'Allez dans votre profil, cliquez sur "Modifier" et remplissez les champs.', 'HELP', 'b0000000-0000-0000-0000-000000000001', NOW() - INTERVAL '2 hours'),
('a0000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000003', 'Quels sont les types d offres disponibles ?', 'REZO propose des stages, emplois et formations selon vos prefereces.', 'OFFER_TYPES', 'b0000000-0000-0000-0000-000000000003', NOW() - INTERVAL '30 minutes');

-- Refresh tokens (pour testing JWT)
INSERT INTO refresh_tokens (id, user_id, token_value, expiration_date, created_at, revoked_at) VALUES
('c0000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 'refresh_token_user2_' || gen_random_uuid()::text, NOW() + INTERVAL '7 days', NOW(), NULL),
('c0000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003', 'refresh_token_user3_' || gen_random_uuid()::text, NOW() + INTERVAL '7 days', NOW(), NULL),
('c0000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000004', 'refresh_token_user4_' || gen_random_uuid()::text, NOW() + INTERVAL '7 days', NOW(), NULL),
('c0000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000006', 'refresh_token_user6_' || gen_random_uuid()::text, NOW() + INTERVAL '7 days', NOW(), NULL),
('c0000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000008', 'refresh_token_user8_' || gen_random_uuid()::text, NOW() + INTERVAL '7 days', NOW(), NULL);

COMMIT;

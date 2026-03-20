-- Init rapide REZO pour tests locaux PostgreSQL
-- Usage:
--   psql -U rezo_user -d rezo_db -f docs/init_rezo_db.sql

CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO packs (id, nom, description, prix, cible, features, created_at)
SELECT
    gen_random_uuid(),
    'FREE',
    'Pack gratuit de demarrage',
    0.00,
    'TOUS',
    'MATCHING_BASIC,MESSAGERIE_LIMITEE',
    now()
WHERE NOT EXISTS (
    SELECT 1
    FROM packs
    WHERE nom = 'FREE'
);

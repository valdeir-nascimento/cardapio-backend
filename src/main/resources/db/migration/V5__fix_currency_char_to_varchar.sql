-- Fix currency columns: CHAR(3) → VARCHAR(3) so Hibernate validation passes
-- PostgreSQL stores CHAR(3) as bpchar which Hibernate 6 maps to Types#CHAR,
-- but @Column(length=3) on String maps to Types#VARCHAR. Use VARCHAR consistently.
ALTER TABLE products          ALTER COLUMN currency TYPE VARCHAR(3);
ALTER TABLE product_variations ALTER COLUMN currency TYPE VARCHAR(3);
ALTER TABLE addon_items       ALTER COLUMN currency TYPE VARCHAR(3);

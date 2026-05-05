-- ============================================================================
-- Massa de teste — repeatable migration (rodada apenas em profile dev).
--
-- Localização ativada via application-dev.yml:
--   spring.flyway.locations: classpath:db/migration,classpath:db/seed
--
-- Características:
--   - UUIDs gerados pelo PostgreSQL com gen_random_uuid() (built-in em PG 13+).
--   - Idempotente: TRUNCATE limpa dados de domínio antes de reinserir.
--     Como é uma repeatable migration, só reexecuta quando o checksum mudar.
--   - Não toca em flyway_schema_history nem em event_publication.
--   - Referências entre tabelas usam chaves naturais (nome de categoria,
--     nome de produto, nome de grupo de adicional).
--
-- Senhas (BCrypt, strength 12):
--   admin@cardapio.local      -> Admin@123     (roles: OWNER)
--   manager@cardapio.local    -> Admin@123     (roles: MANAGER)
--   operator@cardapio.local   -> Admin@123     (roles: OPERATOR)
--   maria@example.com         -> Customer@123
--   joao@example.com          -> Customer@123
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Limpeza (apenas tabelas de domínio + tokens; preserva schema_history e events)
-- ----------------------------------------------------------------------------
TRUNCATE TABLE
    addon_items,
    addon_groups,
    product_variations,
    products,
    categories,
    operating_hours,
    refresh_tokens,
    customers,
    admins
RESTART IDENTITY CASCADE;

-- ----------------------------------------------------------------------------
-- ADMINS
-- ----------------------------------------------------------------------------
INSERT INTO admins (id, name, email, password_hash, roles, created_at) VALUES
    (gen_random_uuid(), 'Owner Cardápio',   'admin@cardapio.local',
     '$2b$12$HaMAOe6p6X.g9D9kvKSW.OeCEI9061JT/BPtyGUAB89Sg3SUJ.rpO', 'OWNER',    NOW()),
    (gen_random_uuid(), 'Manager Cardápio', 'manager@cardapio.local',
     '$2b$12$HaMAOe6p6X.g9D9kvKSW.OeCEI9061JT/BPtyGUAB89Sg3SUJ.rpO', 'MANAGER',  NOW()),
    (gen_random_uuid(), 'Operador Caixa',   'operator@cardapio.local',
     '$2b$12$HaMAOe6p6X.g9D9kvKSW.OeCEI9061JT/BPtyGUAB89Sg3SUJ.rpO', 'OPERATOR', NOW());

-- ----------------------------------------------------------------------------
-- CUSTOMERS
-- ----------------------------------------------------------------------------
INSERT INTO customers (id, name, email, phone_number, password_hash, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Maria Silva', 'maria@example.com', '+5511912345678',
     '$2b$12$okb.Ap5tZ.TgD//pwJ1FD.mNHr6AfXZAuTSFhZmfAic.keYVfCFnu', NOW(), NOW()),
    (gen_random_uuid(), 'João Souza',  'joao@example.com',  '+5511987654321',
     '$2b$12$okb.Ap5tZ.TgD//pwJ1FD.mNHr6AfXZAuTSFhZmfAic.keYVfCFnu', NOW(), NOW());

-- ----------------------------------------------------------------------------
-- CATEGORIES
-- ----------------------------------------------------------------------------
INSERT INTO categories (id, name, display_order, active, created_at) VALUES
    (gen_random_uuid(), 'Pizzas',          1,  TRUE,  NOW()),
    (gen_random_uuid(), 'Hambúrgueres',    2,  TRUE,  NOW()),
    (gen_random_uuid(), 'Bebidas',         3,  TRUE,  NOW()),
    (gen_random_uuid(), 'Sobremesas',      4,  TRUE,  NOW()),
    (gen_random_uuid(), 'Combos Inativos', 99, FALSE, NOW());

-- ----------------------------------------------------------------------------
-- PRODUCTS (category_id resolvido por nome)
-- ----------------------------------------------------------------------------
INSERT INTO products (id, category_id, name, description, base_price, currency, image_url,
                      available, allows_half_half, stock_quantity, created_at, updated_at)
SELECT gen_random_uuid(), c.id, p.name, p.description, p.base_price, 'BRL', p.image_url,
       p.available, p.allows_half_half, p.stock_quantity, NOW(), NOW()
FROM categories c
JOIN (VALUES
    -- (category_name,    name,                            description,                                                                       base_price, image_url,                                            available, allows_half_half, stock_quantity)
    ('Pizzas',         'Pizza Margherita',              'Molho de tomate, mussarela de búfala, manjericão fresco e azeite extra virgem.', 49.90, 'https://cdn.example.com/pizzas/margherita.jpg',     TRUE,  TRUE,  NULL::INT),
    ('Pizzas',         'Pizza Calabresa',               'Molho de tomate, mussarela, calabresa fatiada e cebola roxa.',                   45.90, 'https://cdn.example.com/pizzas/calabresa.jpg',      TRUE,  TRUE,  NULL::INT),
    ('Pizzas',         'Pizza Quatro Queijos',          'Mussarela, provolone, parmesão e gorgonzola.',                                   54.90, 'https://cdn.example.com/pizzas/quatroqueijos.jpg',  TRUE,  TRUE,  NULL::INT),
    ('Hambúrgueres',   'Smash Burger Duplo',            'Dois smashes de 90g, queijo cheddar, picles e molho da casa no pão brioche.',    32.50, 'https://cdn.example.com/burgers/smash.jpg',         TRUE,  FALSE, 50),
    ('Hambúrgueres',   'Cheeseburger Clássico',         'Hambúrguer 150g, cheddar, alface e tomate.',                                     24.00, NULL,                                                 TRUE,  FALSE, 30),
    ('Bebidas',        'Coca-Cola 350ml',               'Lata gelada.',                                                                    7.50, NULL,                                                 TRUE,  FALSE, 200),
    ('Bebidas',        'Suco Natural de Laranja 500ml', 'Espremido na hora.',                                                             12.00, NULL,                                                 TRUE,  FALSE, 25),
    ('Sobremesas',     'Petit Gâteau',                  'Bolinho quente de chocolate com sorvete de creme.',                              19.90, NULL,                                                 TRUE,  FALSE, 15),
    ('Sobremesas',     'Pudim de Leite',                'Pudim caseiro com calda de caramelo.',                                           14.90, NULL,                                                 FALSE, FALSE, 0)
) AS p(category_name, name, description, base_price, image_url, available, allows_half_half, stock_quantity)
  ON c.name = p.category_name;

-- ----------------------------------------------------------------------------
-- VARIATIONS (product_id resolvido por nome)
-- ----------------------------------------------------------------------------
INSERT INTO product_variations (id, product_id, name, price_modifier, currency, position)
SELECT gen_random_uuid(), p.id, v.name, v.price_modifier, 'BRL', v.position
FROM products p
JOIN (VALUES
    ('Pizza Margherita',     'Pequena (4 fatias)', 0.00,  0),
    ('Pizza Margherita',     'Média (6 fatias)',   8.00,  1),
    ('Pizza Margherita',     'Grande (8 fatias)',  15.00, 2),
    ('Pizza Calabresa',      'Pequena (4 fatias)', 0.00,  0),
    ('Pizza Calabresa',      'Média (6 fatias)',   7.00,  1),
    ('Pizza Calabresa',      'Grande (8 fatias)',  14.00, 2),
    ('Pizza Quatro Queijos', 'Média (6 fatias)',   0.00,  0),
    ('Pizza Quatro Queijos', 'Grande (8 fatias)',  12.00, 1)
) AS v(product_name, name, price_modifier, position)
  ON p.name = v.product_name;

-- ----------------------------------------------------------------------------
-- ADDON GROUPS (product_id resolvido por nome)
-- ----------------------------------------------------------------------------
INSERT INTO addon_groups (id, product_id, name, min_selection, max_selection, position)
SELECT gen_random_uuid(), p.id, g.name, g.min_selection, g.max_selection, g.position
FROM products p
JOIN (VALUES
    ('Pizza Margherita',  'Adicionais',      0, 5, 0),
    ('Pizza Margherita',  'Borda recheada',  0, 1, 1),
    ('Smash Burger Duplo','Adicionais',      0, 4, 0),
    ('Smash Burger Duplo','Ponto da carne',  1, 1, 1)
) AS g(product_name, name, min_selection, max_selection, position)
  ON p.name = g.product_name;

-- ----------------------------------------------------------------------------
-- ADDON ITEMS (addon_group_id resolvido por (produto, grupo))
-- ----------------------------------------------------------------------------
INSERT INTO addon_items (id, addon_group_id, name, price, currency, position)
SELECT gen_random_uuid(), ag.id, i.name, i.price, 'BRL', i.position
FROM addon_groups ag
JOIN products p ON p.id = ag.product_id
JOIN (VALUES
    ('Pizza Margherita',   'Adicionais',     'Bacon',                4.50, 0),
    ('Pizza Margherita',   'Adicionais',     'Cebola caramelizada',  3.00, 1),
    ('Pizza Margherita',   'Adicionais',     'Manjericão extra',     2.00, 2),
    ('Pizza Margherita',   'Borda recheada', 'Catupiry',             8.00, 0),
    ('Pizza Margherita',   'Borda recheada', 'Cheddar',              7.00, 1),
    ('Smash Burger Duplo', 'Adicionais',     'Bacon crocante',       5.00, 0),
    ('Smash Burger Duplo', 'Adicionais',     'Cheddar extra',        4.00, 1),
    ('Smash Burger Duplo', 'Adicionais',     'Ovo',                  3.50, 2),
    ('Smash Burger Duplo', 'Ponto da carne', 'Mal passado',          0.00, 0),
    ('Smash Burger Duplo', 'Ponto da carne', 'Ao ponto',             0.00, 1),
    ('Smash Burger Duplo', 'Ponto da carne', 'Bem passado',          0.00, 2)
) AS i(product_name, group_name, name, price, position)
  ON p.name = i.product_name AND ag.name = i.group_name;

-- ----------------------------------------------------------------------------
-- OPERATING HOURS
--   Seg-Qui: 11:00-15:00 e 18:00-23:00
--   Sex:     11:00-15:00 e 18:00-23:30
--   Sáb:     11:00-23:30
--   Dom:     17:00-22:00
-- ----------------------------------------------------------------------------
INSERT INTO operating_hours (id, day_of_week, open_time, close_time) VALUES
    (gen_random_uuid(), 1, '11:00', '15:00'),
    (gen_random_uuid(), 1, '18:00', '23:00'),
    (gen_random_uuid(), 2, '11:00', '15:00'),
    (gen_random_uuid(), 2, '18:00', '23:00'),
    (gen_random_uuid(), 3, '11:00', '15:00'),
    (gen_random_uuid(), 3, '18:00', '23:00'),
    (gen_random_uuid(), 4, '11:00', '15:00'),
    (gen_random_uuid(), 4, '18:00', '23:00'),
    (gen_random_uuid(), 5, '11:00', '15:00'),
    (gen_random_uuid(), 5, '18:00', '23:30'),
    (gen_random_uuid(), 6, '11:00', '23:30'),
    (gen_random_uuid(), 7, '17:00', '22:00');

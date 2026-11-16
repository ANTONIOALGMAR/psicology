INSERT INTO users (id, name, email, password_hash, role, enabled, created_at)
VALUES (
    'aaaaaaaa-0000-0000-0000-000000000001',
    'Administrador',
    'admin@psicare.com',
    '$2y$10$fw9usgC7AAi2JmCTTY.G7O/Eoecek6TVH72l3HYsCKgmqLTbmg69a',
    'ADMIN',
    TRUE,
    NOW()
)
ON CONFLICT (email) DO NOTHING;

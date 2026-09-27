INSERT INTO ticket_categories (id, name, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Technical Support', now(), now()),
    (gen_random_uuid(), 'Billing',           now(), now()),
    (gen_random_uuid(), 'Account',           now(), now()),
    (gen_random_uuid(), 'General',           now(), now());
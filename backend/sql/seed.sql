INSERT INTO users (
    first_name,
    last_name,
    username,
    password_hash,
    role,
    must_change_password,
    active
)
VALUES
    (
        'Test',
        'Framer',
        'framer',
        '$2a$10$tc65dJko2GSG46t1f5z9N.CSorMASMde.YVj6cFATl58wSoONxLF2',
        'framer',
        FALSE,
        TRUE
    ),
    (
        'Test',
        'Admin',
        'admin',
        '$2a$10$dZrbhITcuaNvz5WuR/fhbejpopKCZqZwwZAnCeNoz9Iz.fPZ8Mr8W',
        'admin',
        FALSE,
        TRUE
    );


INSERT INTO safety_checklists (name)
VALUES ('Standard Framing Safety Checklist');


INSERT INTO safety_checklist_items (
    safety_checklist_id,
    item
)
VALUES
    (1, 'Hard hat worn'),
    (1, 'Safety vest worn'),
    (1, 'Safety boots worn'),
    (1, 'Eye protection worn'),
    (1, 'Fall protection in place'),
    (1, 'Ladders and scaffolding inspected'),
    (1, 'Tools and cords in good condition'),
    (1, 'Hazards identified');


INSERT INTO sites (
    name,
    safety_checklist_id
)
VALUES
    ('Kestrel Ridge', 1),
    ('Harbour View', 1),
    ('Cedar Heights', 1);

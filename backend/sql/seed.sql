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
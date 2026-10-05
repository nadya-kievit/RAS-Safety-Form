CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('admin', 'framer')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE safety_checklists (
    id SERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL
);


CREATE TABLE safety_checklist_items (
    id SERIAL PRIMARY KEY,
    safety_checklist_id INTEGER NOT NULL,
    item TEXT NOT NULL,

    CONSTRAINT fk_checklist_item_checklist
        FOREIGN KEY (safety_checklist_id)
        REFERENCES safety_checklists(id)
        ON DELETE CASCADE
);


CREATE TABLE sites (
    id SERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    safety_checklist_id INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_site_checklist
        FOREIGN KEY (safety_checklist_id)
        REFERENCES safety_checklists(id)
);


CREATE TABLE safety_forms (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    site_id INTEGER NOT NULL,
    form_date DATE NOT NULL,
    notes TEXT,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_safety_form_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_safety_form_site
        FOREIGN KEY (site_id)
        REFERENCES sites(id)
);


CREATE TABLE photos (
    id SERIAL PRIMARY KEY,
    safety_form_id INTEGER NOT NULL,
    storage_path TEXT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_photo_form
        FOREIGN KEY (safety_form_id)
        REFERENCES safety_forms(id)
        ON DELETE CASCADE
);
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


CREATE TABLE site_assignments (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    site_id INTEGER NOT NULL,
    assignment_date DATE NOT NULL,

    CONSTRAINT fk_assignment_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_assignment_site
        FOREIGN KEY (site_id)
        REFERENCES sites(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_site_assignment
        UNIQUE (user_id, site_id, assignment_date)
);


CREATE TABLE safety_forms (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    site_id INTEGER NOT NULL,
    form_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'submitted'
        CHECK (status IN ('submitted', 'reviewed')),
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


CREATE TABLE safety_form_responses (
    id SERIAL PRIMARY KEY,
    safety_form_id INTEGER NOT NULL,
    checklist_item_id INTEGER NOT NULL,
    response BOOLEAN NOT NULL,

    CONSTRAINT fk_response_form
        FOREIGN KEY (safety_form_id)
        REFERENCES safety_forms(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_response_item
        FOREIGN KEY (checklist_item_id)
        REFERENCES safety_checklist_items(id),

    CONSTRAINT uq_form_item_response
        UNIQUE (safety_form_id, checklist_item_id)
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
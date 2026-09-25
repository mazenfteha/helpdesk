CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT chk_users_role
        CHECK (role IN ('CUSTOMER', 'AGENT', 'ADMIN'))
);


CREATE TABLE ticket_categories (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);


CREATE TABLE tickets (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    customer_id UUID NOT NULL,
    category_id UUID NOT NULL,
    assigned_to UUID NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_tickets_customer
        FOREIGN KEY (customer_id)
        REFERENCES users(id),

    CONSTRAINT fk_tickets_category
        FOREIGN KEY (category_id)
        REFERENCES ticket_categories(id),

    CONSTRAINT fk_tickets_assigned_to
        FOREIGN KEY (assigned_to)
        REFERENCES users(id),

    CONSTRAINT chk_tickets_status
        CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')),

    CONSTRAINT chk_tickets_priority
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT'))
);


CREATE TABLE comments (
    id UUID PRIMARY KEY,
    content TEXT NOT NULL,
    ticket_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_comments_ticket
        FOREIGN KEY (ticket_id)
        REFERENCES tickets(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_comments_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);


CREATE INDEX idx_tickets_customer_id
    ON tickets(customer_id);

CREATE INDEX idx_tickets_assigned_to
    ON tickets(assigned_to);

CREATE INDEX idx_tickets_status
    ON tickets(status);

CREATE INDEX idx_tickets_category_id
    ON tickets(category_id);
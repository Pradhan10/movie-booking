-- V1__Create_base_schema.sql

-- Theatre Partner
CREATE TABLE theatre_partner (
    id BIGSERIAL PRIMARY KEY,
    company_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    contact_person VARCHAR(255),
    status VARCHAR(20) DEFAULT 'PENDING',
    commission_rate DECIMAL(5,2) DEFAULT 10.00,
    onboarding_date DATE,
    contract_start DATE,
    contract_end DATE,
    api_key VARCHAR(255) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_theatre_partner_email ON theatre_partner(email);
CREATE INDEX idx_theatre_partner_status ON theatre_partner(status);

-- Theatre
CREATE TABLE theatre (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100),
    country VARCHAR(100) DEFAULT 'India',
    address TEXT,
    partner_id BIGINT NOT NULL,
    total_screens INT NOT NULL,
    facilities JSONB,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (partner_id) REFERENCES theatre_partner(id)
);

CREATE INDEX idx_theatre_city ON theatre(city);
CREATE INDEX idx_theatre_partner ON theatre(partner_id);

-- Movie
CREATE TABLE movie (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    language VARCHAR(50) NOT NULL,
    genre VARCHAR(100),
    duration_minutes INT NOT NULL,
    release_date DATE NOT NULL,
    rating VARCHAR(10),
    poster_url VARCHAR(500),
    description TEXT,
    cast JSONB,
    director VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_movie_language ON movie(language);
CREATE INDEX idx_movie_genre ON movie(genre);
CREATE INDEX idx_movie_release_date ON movie(release_date);

-- Screen
CREATE TABLE screen (
    id BIGSERIAL PRIMARY KEY,
    theatre_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    total_seats INT NOT NULL,
    seat_layout JSONB NOT NULL,
    screen_type VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (theatre_id) REFERENCES theatre(id)
);

CREATE INDEX idx_screen_theatre ON screen(theatre_id);

-- Show
CREATE TABLE show (
    id BIGSERIAL PRIMARY KEY,
    movie_id BIGINT NOT NULL,
    theatre_id BIGINT NOT NULL,
    screen_id BIGINT NOT NULL,
    show_date DATE NOT NULL,
    show_time TIME NOT NULL,
    base_price DECIMAL(10,2) NOT NULL,
    available_seats INT NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    version INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (movie_id) REFERENCES movie(id),
    FOREIGN KEY (theatre_id) REFERENCES theatre(id),
    FOREIGN KEY (screen_id) REFERENCES screen(id),
    CONSTRAINT unique_show_time UNIQUE (screen_id, show_date, show_time)
);

CREATE INDEX idx_show_movie_date ON show(movie_id, show_date);
CREATE INDEX idx_show_theatre_date ON show(theatre_id, show_date);
CREATE INDEX idx_show_datetime ON show(show_date, show_time);

-- Seat
CREATE TABLE seat (
    id BIGSERIAL PRIMARY KEY,
    show_id BIGINT NOT NULL,
    seat_number VARCHAR(10) NOT NULL,
    row_label VARCHAR(5) NOT NULL,
    category VARCHAR(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'AVAILABLE',
    version INT DEFAULT 0,
    held_until TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (show_id) REFERENCES show(id),
    CONSTRAINT unique_seat_per_show UNIQUE (show_id, seat_number)
);

CREATE INDEX idx_seat_show_status ON seat(show_id, status);
CREATE INDEX idx_seat_held_until ON seat(held_until);

-- User Account
CREATE TABLE user_account (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    preferred_language VARCHAR(50),
    preferred_city VARCHAR(100),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);

CREATE INDEX idx_user_email ON user_account(email);
CREATE INDEX idx_user_phone ON user_account(phone);

-- Booking
CREATE TABLE booking (
    id BIGSERIAL PRIMARY KEY,
    booking_reference VARCHAR(20) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    show_id BIGINT NOT NULL,
    booking_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(10,2) NOT NULL,
    discount_amount DECIMAL(10,2) DEFAULT 0,
    final_amount DECIMAL(10,2) NOT NULL,
    offer_applied VARCHAR(100),
    status VARCHAR(20) DEFAULT 'PENDING',
    payment_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user_account(id),
    FOREIGN KEY (show_id) REFERENCES show(id)
);

CREATE INDEX idx_booking_user ON booking(user_id);
CREATE INDEX idx_booking_show ON booking(show_id);
CREATE INDEX idx_booking_reference ON booking(booking_reference);
CREATE INDEX idx_booking_status_date ON booking(status, booking_date);

-- Booking Seat
CREATE TABLE booking_seat (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    price_paid DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES booking(id),
    FOREIGN KEY (seat_id) REFERENCES seat(id),
    CONSTRAINT unique_booking_seat UNIQUE (booking_id, seat_id)
);

CREATE INDEX idx_booking_seat_booking ON booking_seat(booking_id);
CREATE INDEX idx_booking_seat_seat ON booking_seat(seat_id);

-- Payment
CREATE TABLE payment (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(50),
    gateway_name VARCHAR(50),
    gateway_txn_id VARCHAR(255),
    status VARCHAR(20) DEFAULT 'INITIATED',
    gateway_response TEXT,
    initiated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    failed_reason TEXT,
    FOREIGN KEY (booking_id) REFERENCES booking(id)
);

CREATE INDEX idx_payment_booking ON payment(booking_id);
CREATE INDEX idx_payment_gateway_txn ON payment(gateway_txn_id);
CREATE INDEX idx_payment_status ON payment(status);

-- Offer
CREATE TABLE offer (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    conditions TEXT,
    valid_from DATE NOT NULL,
    valid_to DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    max_usage_per_user INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_offer_code ON offer(code);
CREATE INDEX idx_offer_validity ON offer(valid_from, valid_to);
CREATE INDEX idx_offer_status ON offer(status);

-- Notification
CREATE TABLE notification (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255),
    message TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    sent_at TIMESTAMP,
    failed_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES booking(id),
    FOREIGN KEY (user_id) REFERENCES user_account(id)
);

CREATE INDEX idx_notification_booking ON notification(booking_id);
CREATE INDEX idx_notification_status_retry ON notification(status, retry_count);
CREATE INDEX idx_notification_created_at ON notification(created_at);

-- Booking Audit
CREATE TABLE booking_audit (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES booking(id)
);

CREATE INDEX idx_booking_audit_booking ON booking_audit(booking_id);
CREATE INDEX idx_booking_audit_action ON booking_audit(action);
CREATE INDEX idx_booking_audit_created_at ON booking_audit(created_at);

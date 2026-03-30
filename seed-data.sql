-- Seed data for testing

-- Insert theatre partners
INSERT INTO theatre_partner (company_name, email, phone, contact_person, status, commission_rate, onboarding_date) VALUES
('PVR Cinemas', 'contact@pvrcinemas.com', '+91-9876543210', 'Ajay Kumar', 'ACTIVE', 8.5, '2024-01-15'),
('INOX Leisure', 'info@inoxmovies.com', '+91-9876543211', 'Priya Sharma', 'ACTIVE', 9.0, '2024-02-01'),
('Cinepolis India', 'partnerships@cinepolis.in', '+91-9876543212', 'Rahul Mehta', 'ACTIVE', 8.0, '2024-03-10');

-- Insert theatres
INSERT INTO theatre (name, location, city, state, country, address, partner_id, total_screens, status) VALUES
('PVR Phoenix Marketcity', 'Whitefield', 'Bangalore', 'Karnataka', 'India', 'Phoenix Marketcity Mall, Whitefield Road', 1, 8, 'ACTIVE'),
('INOX Garuda Mall', 'Magrath Road', 'Bangalore', 'Karnataka', 'India', 'Garuda Mall, Magrath Road', 2, 6, 'ACTIVE'),
('Cinepolis Royal Meenakshi', 'Bannerghatta Road', 'Bangalore', 'Karnataka', 'India', 'Royal Meenakshi Mall', 3, 5, 'ACTIVE'),
('PVR Select City Walk', 'Saket', 'Delhi', 'Delhi', 'India', 'Select City Walk Mall, Saket', 1, 10, 'ACTIVE'),
('INOX Nexus Seawoods', 'Navi Mumbai', 'Mumbai', 'Maharashtra', 'India', 'Nexus Seawoods Mall', 2, 7, 'ACTIVE');

-- Insert movies
INSERT INTO movie (title, language, genre, duration_minutes, release_date, rating, description, director) VALUES
('Inception', 'English', 'Sci-Fi, Thriller', 148, '2010-07-16', 'PG-13', 'A thief who steals corporate secrets through dream-sharing technology', 'Christopher Nolan'),
('3 Idiots', 'Hindi', 'Comedy, Drama', 170, '2009-12-25', 'PG', 'Two friends embark on a quest for a lost buddy', 'Rajkumar Hirani'),
('RRR', 'Telugu', 'Action, Drama', 187, '2022-03-25', 'PG-13', 'A fearless warrior on a perilous mission', 'S. S. Rajamouli'),
('The Dark Knight', 'English', 'Action, Crime', 152, '2008-07-18', 'PG-13', 'Batman faces the Joker in Gotham City', 'Christopher Nolan'),
('Dangal', 'Hindi', 'Biography, Drama', 161, '2016-12-23', 'PG', 'Former wrestler trains his daughters', 'Nitesh Tiwari');

-- Insert screens
INSERT INTO screen (theatre_id, name, total_seats, seat_layout, screen_type) VALUES
(1, 'Screen 1', 200, '{"rows": ["A","B","C","D","E"], "seatsPerRow": 40}', 'IMAX'),
(1, 'Screen 2', 150, '{"rows": ["A","B","C","D"], "seatsPerRow": 37}', 'Standard'),
(2, 'Audi 1', 180, '{"rows": ["A","B","C","D"], "seatsPerRow": 45}', 'Standard'),
(3, 'Screen 1', 120, '{"rows": ["A","B","C"], "seatsPerRow": 40}', '4DX'),
(4, 'Screen 5', 250, '{"rows": ["A","B","C","D","E","F"], "seatsPerRow": 42}', 'IMAX');

-- Insert shows (for next 7 days)
INSERT INTO show (movie_id, theatre_id, screen_id, show_date, show_time, base_price, available_seats, status) VALUES
-- Inception shows in Bangalore
(1, 1, 1, CURRENT_DATE + 1, '10:00', 350.00, 200, 'ACTIVE'),
(1, 1, 1, CURRENT_DATE + 1, '13:30', 300.00, 200, 'ACTIVE'),
(1, 1, 1, CURRENT_DATE + 1, '18:30', 350.00, 200, 'ACTIVE'),
(1, 1, 1, CURRENT_DATE + 1, '21:45', 400.00, 200, 'ACTIVE'),

-- 3 Idiots shows
(2, 2, 3, CURRENT_DATE + 1, '11:00', 250.00, 180, 'ACTIVE'),
(2, 2, 3, CURRENT_DATE + 1, '14:00', 220.00, 180, 'ACTIVE'),
(2, 2, 3, CURRENT_DATE + 1, '19:00', 280.00, 180, 'ACTIVE'),

-- RRR shows
(3, 3, 4, CURRENT_DATE + 1, '12:00', 280.00, 120, 'ACTIVE'),
(3, 3, 4, CURRENT_DATE + 1, '15:30', 250.00, 120, 'ACTIVE'),
(3, 3, 4, CURRENT_DATE + 1, '20:00', 320.00, 120, 'ACTIVE');

-- Insert sample users
INSERT INTO user_account (email, phone, name, password_hash, preferred_language, preferred_city, status) VALUES
('john.doe@example.com', '+91-9876501234', 'John Doe', '$2a$10$encrypted_password_hash', 'English', 'Bangalore', 'ACTIVE'),
('priya.sharma@example.com', '+91-9876501235', 'Priya Sharma', '$2a$10$encrypted_password_hash', 'Hindi', 'Delhi', 'ACTIVE'),
('raj.patel@example.com', '+91-9876501236', 'Raj Patel', '$2a$10$encrypted_password_hash', 'English', 'Mumbai', 'ACTIVE');

-- Insert offers
INSERT INTO offer (code, description, discount_type, discount_value, conditions, valid_from, valid_to, status) VALUES
('THIRD50', '50% discount on the third ticket', 'PERCENTAGE', 50.00, 'Third ticket discount', CURRENT_DATE, CURRENT_DATE + 30, 'ACTIVE'),
('AFTERNOON20', '20% discount on afternoon shows (12 PM - 4 PM)', 'PERCENTAGE', 20.00, 'Afternoon show discount', CURRENT_DATE, CURRENT_DATE + 30, 'ACTIVE'),
('WEEKEND25', '25% off on weekend bookings', 'PERCENTAGE', 25.00, 'Weekend special', CURRENT_DATE, CURRENT_DATE + 7, 'ACTIVE');

-- Insert sample seats for show 1 (Inception IMAX show)
-- Creating 200 seats: 5 rows x 40 seats each
DO $$
DECLARE
    show_id_var BIGINT := 1;
    row_labels TEXT[] := ARRAY['A', 'B', 'C', 'D', 'E'];
    row_label TEXT;
    seat_num INT;
    category_val VARCHAR(20);
BEGIN
    FOREACH row_label IN ARRAY row_labels
    LOOP
        FOR seat_num IN 1..40
        LOOP
            -- First 10 seats in each row are PREMIUM, rest are NORMAL
            IF seat_num <= 10 THEN
                category_val := 'PREMIUM';
            ELSE
                category_val := 'NORMAL';
            END IF;
            
            INSERT INTO seat (show_id, seat_number, row_label, category, status, version)
            VALUES (show_id_var, row_label || seat_num, row_label, category_val, 'AVAILABLE', 0);
        END LOOP;
    END LOOP;
END $$;

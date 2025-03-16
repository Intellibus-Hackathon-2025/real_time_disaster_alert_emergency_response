-- Create Users Table
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       first_name VARCHAR(100) NOT NULL,
                       last_name VARCHAR(100) NOT NULL,
                       username VARCHAR(100) UNIQUE NOT NULL,
                       email VARCHAR(100) UNIQUE NOT NULL,
                       phone_number VARCHAR(20),
                       password TEXT NOT NULL,
                       permanent_address TEXT,
                       live_address TEXT,
                       provider VARCHAR(50),
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Add Indexes for Fast Querying
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone_number ON users(phone_number);
CREATE INDEX idx_users_provider ON users(provider);

-- Create Roles Table
CREATE TABLE roles (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(50) UNIQUE NOT NULL
);

-- Add Index for Fast Lookup
CREATE INDEX idx_roles_name ON roles(name);

-- Create User Roles Mapping Table (Many-to-Many)
CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL,
                            role_id BIGINT NOT NULL,
                            PRIMARY KEY (user_id, role_id),
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                            FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Add Indexes for Join Performance
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

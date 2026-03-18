CREATE TABLE profiles (
    id           UUID PRIMARY KEY,
    username     VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(100),
    bio          TEXT,
    avatar_url   VARCHAR(512),
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_profiles_username ON profiles(username);

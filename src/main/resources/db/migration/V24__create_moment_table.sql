CREATE TABLE moments (
    id BIGSERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    tags VARCHAR(1000),
    user_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_moments_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_moments_user_id ON moments(user_id);
CREATE INDEX idx_moments_created_at ON moments(created_at);
CREATE INDEX idx_moments_user_created ON moments(user_id, created_at DESC);
CREATE INDEX idx_moments_tags ON moments(tags);
-- V4: Duels table + extend submissions with duel reference

CREATE TABLE duels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    player1_id UUID NOT NULL REFERENCES users(id),
    player2_id UUID NOT NULL REFERENCES users(id),
    challenge_id UUID NOT NULL REFERENCES challenges(id),
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    finish_reason VARCHAR(20),
    winner_id UUID REFERENCES users(id),
    difficulty VARCHAR(20) NOT NULL,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_duels_player1 ON duels(player1_id);
CREATE INDEX idx_duels_player2 ON duels(player2_id);
CREATE INDEX idx_duels_status ON duels(status);

-- Extend submissions with optional duel reference
ALTER TABLE submissions ADD COLUMN duel_id UUID REFERENCES duels(id);
CREATE INDEX idx_submissions_duel ON submissions(duel_id);

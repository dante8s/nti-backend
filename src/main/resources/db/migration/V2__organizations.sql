CREATE TABLE organizations (
                               id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               name            VARCHAR(255) NOT NULL,
                               ico             VARCHAR(20)  NOT NULL UNIQUE,
                               sector          VARCHAR(100),
                               description     TEXT,
                               contact_email   VARCHAR(255),
                               contact_phone   VARCHAR(50),
                               website         VARCHAR(255),
                               status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
                               created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                               updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE org_members (
                             id              UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
                             organization_id UUID    NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
                             user_id         BIGINT  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                             role            VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
                             joined_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                             CONSTRAINT uq_org_user UNIQUE (organization_id, user_id)
);

CREATE INDEX idx_org_members_org_id  ON org_members(organization_id);
CREATE INDEX idx_org_members_user_id ON org_members(user_id);
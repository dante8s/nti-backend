-- Articles / News
CREATE TABLE cms_articles (
                              id              BIGSERIAL     PRIMARY KEY,
                              title           VARCHAR(500)  NOT NULL,
                              category        VARCHAR(100),
                              excerpt         TEXT,
                              content         TEXT          NOT NULL,
                              image_path      VARCHAR(512),
                              image_name      VARCHAR(255),
                              published       BOOLEAN       NOT NULL DEFAULT FALSE,
                              published_at    TIMESTAMPTZ,
                              created_by_id   BIGINT        REFERENCES users(id) ON DELETE SET NULL,
                              created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
                              updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cms_articles_published    ON cms_articles(published);
CREATE INDEX idx_cms_articles_category     ON cms_articles(category);
CREATE INDEX idx_cms_articles_published_at ON cms_articles(published_at DESC);

-- Successful projects showcase
CREATE TABLE cms_projects (
                              id              BIGSERIAL     PRIMARY KEY,
                              title           VARCHAR(255)  NOT NULL,
                              description     TEXT,
                              image_path      VARCHAR(512),
                              image_name      VARCHAR(255),
                              funding_amount  VARCHAR(50),
                              status_label    VARCHAR(50),
                              sort_order      INTEGER       NOT NULL DEFAULT 0,
                              published       BOOLEAN       NOT NULL DEFAULT TRUE,
                              created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
                              updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- Testimonials / quotes
CREATE TABLE cms_testimonials (
                                  id              BIGSERIAL     PRIMARY KEY,
                                  quote           TEXT          NOT NULL,
                                  author_name     VARCHAR(255)  NOT NULL,
                                  author_role     VARCHAR(255),
                                  avatar_path     VARCHAR(512),
                                  avatar_name     VARCHAR(255),
                                  sort_order      INTEGER       NOT NULL DEFAULT 0,
                                  published       BOOLEAN       NOT NULL DEFAULT TRUE,
                                  created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
                                  updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- Flexible page sections (About NTI, Home, any page)
CREATE TABLE cms_page_sections (
                                   id              BIGSERIAL     PRIMARY KEY,
                                   page_key        VARCHAR(100)  NOT NULL,
                                   section_type    VARCHAR(50)   NOT NULL DEFAULT 'block',
                                   title           VARCHAR(500),
                                   subtitle        VARCHAR(500),
                                   content         TEXT,
                                   icon            VARCHAR(100),
                                   image_path      VARCHAR(512),
                                   image_name      VARCHAR(255),
                                   sort_order      INTEGER       NOT NULL DEFAULT 0,
                                   published       BOOLEAN       NOT NULL DEFAULT TRUE,
                                   created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
                                   updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cms_page_sections_page_key ON cms_page_sections(page_key);
CREATE INDEX idx_cms_page_sections_sort     ON cms_page_sections(page_key, sort_order);
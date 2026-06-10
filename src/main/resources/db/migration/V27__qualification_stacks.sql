CREATE TABLE qualification_stacks (
    id               BIGSERIAL PRIMARY KEY,
    stack_number     INT          NOT NULL,
    specialization_key VARCHAR(50) NOT NULL UNIQUE,
    specialization_name VARCHAR(255) NOT NULL
);

CREATE TABLE qualification_subjects (
    id           BIGSERIAL PRIMARY KEY,
    stack_id     BIGINT       NOT NULL REFERENCES qualification_stacks(id) ON DELETE CASCADE,
    subject_name VARCHAR(255) NOT NULL,
    position     INT          NOT NULL DEFAULT 0
);

INSERT INTO qualification_stacks (stack_number, specialization_key, specialization_name) VALUES
(1, 'software', 'Vývoj softvéru'),
(2, 'ai',       'AI a dátové technológie'),
(3, 'web',      'Webové aplikácie'),
(4, 'game',     'Herný vývoj'),
(5, 'iot',      'IoT a embedded systémy');

INSERT INTO qualification_subjects (stack_id, subject_name, position)
SELECT s.id, subj.name, subj.pos
FROM qualification_stacks s
JOIN (VALUES
    ('software', 'objektové technológie',                  1),
    ('software', 'úvod do softvérového inžinierstva',      2),
    ('software', 'mobilné aplikácie',                      3),
    ('software', 'senzory',                                4),
    ('software', 'manažment projektov',                    5),
    ('software', 'testovanie',                             6),
    ('ai',       'databázové systémy',                     1),
    ('ai',       'počítačová analýza dát',                 2),
    ('ai',       'AI',                                     3),
    ('ai',       'úvod do strojového učenia',              4),
    ('ai',       'neurónové siete',                        5),
    ('ai',       'hĺbková analýza dát',                    6),
    ('web',      'jazyky webu',                            1),
    ('web',      'FE/BE technológie',                      2),
    ('web',      'webové aplikácie na platforme Java',     3),
    ('game',     'herné vývojové prostredia',              1),
    ('game',     'vývoj 3D aplikácií',                     2),
    ('game',     'virtuálna a rozšírená realita',          3),
    ('iot',      'programovanie v jazyku C',               1),
    ('iot',      'internet vecí',                          2),
    ('iot',      'inteligentné systémy',                   3),
    ('iot',      'robotické a priemyselné systémy',        4)
) AS subj(key, name, pos) ON s.specialization_key = subj.key;

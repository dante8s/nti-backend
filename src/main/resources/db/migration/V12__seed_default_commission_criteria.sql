-- Default evaluation rubric for calls without any criteria (weights sum to 100%).
INSERT INTO criteria (call_id, name, description, weight_percent, max_score, sort_order)
SELECT c.id,
       v.name,
       v.description,
       v.weight_percent,
       v.max_score,
       v.sort_order
FROM calls c
CROSS JOIN (
    VALUES
        (1,
         'Originality of the idea',
         'How much the project differs from existing solutions, freshness of the problem formulation and approaches.',
         25,
         100.0),
        (2,
         'Technical feasibility',
         'Realism of the technical plan, architecture and implementation roadmap.',
         25,
         100.0),
        (3,
         'Social and market impact',
         'Value for the audience, social-ecological or economic effect, scaling potential.',
         25,
         100.0),
        (4,
         'Quality and completeness of materials',
         'Structured submitted documentation: budget, risk analysis, monetization and compliance with call requirements.',
         25,
         100.0)
) AS v (sort_order, name, description, weight_percent, max_score)
WHERE NOT EXISTS (SELECT 1 FROM criteria cr WHERE cr.call_id = c.id);

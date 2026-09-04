-- Demo data for the Training Platform.
-- Run this after the Spring Boot backend has started at least once, so Hibernate
-- has created/updated the tables.
--
-- Seeded user password for demo trainer/learner accounts: password
-- Existing admin account remains: admin@training.com / admin123

BEGIN;

INSERT INTO skills (name, description, created_at, updated_at) VALUES
  ('Java', 'Java programming language', now(), now()),
  ('Spring Boot', 'Spring Boot backend development', now(), now()),
  ('Spring Security', 'Authentication, authorization, and JWT security', now(), now()),
  ('REST APIs', 'RESTful API design and implementation', now(), now()),
  ('PostgreSQL', 'Relational database design and SQL with PostgreSQL', now(), now()),
  ('Angular', 'Angular frontend development', now(), now()),
  ('TypeScript', 'Typed JavaScript for frontend applications', now(), now()),
  ('Project Management', 'Planning and managing projects', now(), now()),
  ('Agile', 'Agile delivery, Scrum, and iterative planning', now(), now()),
  ('Communication', 'Professional communication skills', now(), now()),
  ('Business English', 'English for professional environments', now(), now()),
  ('French', 'French language skills', now(), now())
ON CONFLICT (name) DO UPDATE
SET description = EXCLUDED.description,
    updated_at = now();

INSERT INTO categories (name, description, created_at, updated_at) VALUES
  ('IT', 'Information technology, software development, and databases', now(), now()),
  ('Management', 'Leadership, project management, and business skills', now(), now()),
  ('Languages', 'Professional language learning', now(), now()),
  ('Cybersecurity', 'Security, identity, access control, and risk awareness', now(), now())
ON CONFLICT (name) DO UPDATE
SET description = EXCLUDED.description,
    updated_at = now();

INSERT INTO formations (title, description, price, level, duration_hours, active, category_id, created_at, updated_at)
SELECT 'Spring Boot Fundamentals',
       'Build professional REST APIs with Spring Boot, Spring Data JPA, validation, PostgreSQL, and JWT basics.',
       250.00, 'BEGINNER', 24, true, c.id, now(), now()
FROM categories c
WHERE c.name = 'IT'
  AND NOT EXISTS (SELECT 1 FROM formations f WHERE lower(f.title) = lower('Spring Boot Fundamentals'));

INSERT INTO formations (title, description, price, level, duration_hours, active, category_id, created_at, updated_at)
SELECT 'Advanced Spring Security',
       'Secure APIs with JWT, role-based access control, account locking, and password management flows.',
       340.00, 'ADVANCED', 30, true, c.id, now(), now()
FROM categories c
WHERE c.name = 'Cybersecurity'
  AND NOT EXISTS (SELECT 1 FROM formations f WHERE lower(f.title) = lower('Advanced Spring Security'));

INSERT INTO formations (title, description, price, level, duration_hours, active, category_id, created_at, updated_at)
SELECT 'Angular Essentials',
       'Create modern Angular applications with routing, reactive forms, services, guards, and clean UI structure.',
       220.00, 'BEGINNER', 20, true, c.id, now(), now()
FROM categories c
WHERE c.name = 'IT'
  AND NOT EXISTS (SELECT 1 FROM formations f WHERE lower(f.title) = lower('Angular Essentials'));

INSERT INTO formations (title, description, price, level, duration_hours, active, category_id, created_at, updated_at)
SELECT 'Full Stack Java Angular',
       'Connect Spring Boot APIs with an Angular frontend and build a complete professional training platform workflow.',
       420.00, 'INTERMEDIATE', 42, true, c.id, now(), now()
FROM categories c
WHERE c.name = 'IT'
  AND NOT EXISTS (SELECT 1 FROM formations f WHERE lower(f.title) = lower('Full Stack Java Angular'));

INSERT INTO formations (title, description, price, level, duration_hours, active, category_id, created_at, updated_at)
SELECT 'Project Management Professional',
       'Plan projects, manage risks, follow progress, and communicate effectively with stakeholders.',
       300.00, 'INTERMEDIATE', 28, true, c.id, now(), now()
FROM categories c
WHERE c.name = 'Management'
  AND NOT EXISTS (SELECT 1 FROM formations f WHERE lower(f.title) = lower('Project Management Professional'));

INSERT INTO formations (title, description, price, level, duration_hours, active, category_id, created_at, updated_at)
SELECT 'Business Communication',
       'Improve workplace communication, presentations, professional writing, and negotiation skills.',
       180.00, 'INTERMEDIATE', 16, true, c.id, now(), now()
FROM categories c
WHERE c.name = 'Management'
  AND NOT EXISTS (SELECT 1 FROM formations f WHERE lower(f.title) = lower('Business Communication'));

INSERT INTO formations (title, description, price, level, duration_hours, active, category_id, created_at, updated_at)
SELECT 'Professional English for IT',
       'Practice technical English for meetings, documentation, interviews, and daily collaboration.',
       160.00, 'BEGINNER', 18, true, c.id, now(), now()
FROM categories c
WHERE c.name = 'Languages'
  AND NOT EXISTS (SELECT 1 FROM formations f WHERE lower(f.title) = lower('Professional English for IT'));

INSERT INTO formations (title, description, price, level, duration_hours, active, category_id, created_at, updated_at)
SELECT 'French Workplace Communication',
       'Develop French communication skills for business conversations and professional writing.',
       150.00, 'BEGINNER', 18, true, c.id, now(), now()
FROM categories c
WHERE c.name = 'Languages'
  AND NOT EXISTS (SELECT 1 FROM formations f WHERE lower(f.title) = lower('French Workplace Communication'));

-- Required skills per formation.
INSERT INTO formation_required_skills (formation_id, skill_id)
SELECT f.id, s.id
FROM formations f
JOIN skills s ON s.name IN ('Java', 'REST APIs', 'PostgreSQL')
WHERE f.title = 'Spring Boot Fundamentals'
  AND NOT EXISTS (
    SELECT 1 FROM formation_required_skills frs WHERE frs.formation_id = f.id AND frs.skill_id = s.id
  );

INSERT INTO formation_required_skills (formation_id, skill_id)
SELECT f.id, s.id
FROM formations f
JOIN skills s ON s.name IN ('Java', 'Spring Boot', 'Spring Security', 'REST APIs')
WHERE f.title = 'Advanced Spring Security'
  AND NOT EXISTS (
    SELECT 1 FROM formation_required_skills frs WHERE frs.formation_id = f.id AND frs.skill_id = s.id
  );

INSERT INTO formation_required_skills (formation_id, skill_id)
SELECT f.id, s.id
FROM formations f
JOIN skills s ON s.name IN ('Angular', 'TypeScript')
WHERE f.title = 'Angular Essentials'
  AND NOT EXISTS (
    SELECT 1 FROM formation_required_skills frs WHERE frs.formation_id = f.id AND frs.skill_id = s.id
  );

INSERT INTO formation_required_skills (formation_id, skill_id)
SELECT f.id, s.id
FROM formations f
JOIN skills s ON s.name IN ('Java', 'Spring Boot', 'Angular', 'TypeScript', 'REST APIs')
WHERE f.title = 'Full Stack Java Angular'
  AND NOT EXISTS (
    SELECT 1 FROM formation_required_skills frs WHERE frs.formation_id = f.id AND frs.skill_id = s.id
  );

INSERT INTO formation_required_skills (formation_id, skill_id)
SELECT f.id, s.id
FROM formations f
JOIN skills s ON s.name IN ('Project Management', 'Agile', 'Communication')
WHERE f.title = 'Project Management Professional'
  AND NOT EXISTS (
    SELECT 1 FROM formation_required_skills frs WHERE frs.formation_id = f.id AND frs.skill_id = s.id
  );

INSERT INTO formation_required_skills (formation_id, skill_id)
SELECT f.id, s.id
FROM formations f
JOIN skills s ON s.name IN ('Communication', 'Business English')
WHERE f.title = 'Business Communication'
  AND NOT EXISTS (
    SELECT 1 FROM formation_required_skills frs WHERE frs.formation_id = f.id AND frs.skill_id = s.id
  );

INSERT INTO formation_required_skills (formation_id, skill_id)
SELECT f.id, s.id
FROM formations f
JOIN skills s ON s.name IN ('Business English', 'Communication')
WHERE f.title = 'Professional English for IT'
  AND NOT EXISTS (
    SELECT 1 FROM formation_required_skills frs WHERE frs.formation_id = f.id AND frs.skill_id = s.id
  );

INSERT INTO formation_required_skills (formation_id, skill_id)
SELECT f.id, s.id
FROM formations f
JOIN skills s ON s.name IN ('French', 'Communication')
WHERE f.title = 'French Workplace Communication'
  AND NOT EXISTS (
    SELECT 1 FROM formation_required_skills frs WHERE frs.formation_id = f.id AND frs.skill_id = s.id
  );

-- Chapters.
INSERT INTO chapters (title, content, order_index, formation_id, created_at, updated_at)
SELECT chapter_title, chapter_content, order_index, f.id, now(), now()
FROM (
  VALUES
    ('Spring Boot Fundamentals', 1, 'Project setup and architecture', 'Create a clean Spring Boot project and understand layered responsibilities.'),
    ('Spring Boot Fundamentals', 2, 'REST controllers and validation', 'Expose DTO-based endpoints and validate incoming requests.'),
    ('Spring Boot Fundamentals', 3, 'JPA and PostgreSQL', 'Persist entities and query data with Spring Data JPA.'),
    ('Advanced Spring Security', 1, 'JWT authentication flow', 'Understand token generation, validation, and stateless sessions.'),
    ('Advanced Spring Security', 2, 'Role-based authorization', 'Protect endpoints with roles and ownership checks.'),
    ('Advanced Spring Security', 3, 'Account protection', 'Handle failed login attempts, locking, and password reset flows.'),
    ('Angular Essentials', 1, 'Routing and layouts', 'Build feature routes and dashboard layouts.'),
    ('Angular Essentials', 2, 'Reactive forms', 'Create robust forms for login, register, and CRUD pages.'),
    ('Angular Essentials', 3, 'HTTP services and guards', 'Connect Angular services to secured backend APIs.'),
    ('Full Stack Java Angular', 1, 'API integration', 'Connect Angular pages to Spring Boot endpoints.'),
    ('Full Stack Java Angular', 2, 'JWT in frontend', 'Store tokens, attach authorization headers, and guard routes.'),
    ('Project Management Professional', 1, 'Project planning', 'Define scope, milestones, and delivery risks.'),
    ('Project Management Professional', 2, 'Agile execution', 'Run iterations and track team progress.'),
    ('Business Communication', 1, 'Professional writing', 'Write clear emails, summaries, and reports.'),
    ('Business Communication', 2, 'Presentations', 'Prepare and deliver structured presentations.'),
    ('Professional English for IT', 1, 'Technical vocabulary', 'Use common software and project vocabulary in English.'),
    ('French Workplace Communication', 1, 'Business conversations', 'Practice common workplace situations in French.')
) AS data(formation_title, order_index, chapter_title, chapter_content)
JOIN formations f ON f.title = data.formation_title
WHERE NOT EXISTS (
  SELECT 1 FROM chapters c
  WHERE c.formation_id = f.id AND c.order_index = data.order_index AND lower(c.title) = lower(data.chapter_title)
);

-- Demo users. Password for all users below is: password
INSERT INTO users (
  first_name, last_name, email, password, role, enabled,
  failed_login_attempts, account_locked, lock_time, created_at, updated_at
) VALUES
  ('Yassine', 'Trainer', 'trainer.java@training.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'TRAINER', true, 0, false, null, now(), now()),
  ('Ines', 'Frontend', 'trainer.angular@training.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'TRAINER', true, 0, false, null, now(), now()),
  ('Karim', 'Coach', 'trainer.management@training.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'TRAINER', true, 0, false, null, now(), now()),
  ('Amine', 'Ben Ali', 'learner.amine@training.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'LEARNER', true, 0, false, null, now(), now()),
  ('Sara', 'Mansouri', 'learner.sara@training.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'LEARNER', true, 0, false, null, now(), now()),
  ('Nour', 'Haddad', 'learner.nour@training.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'LEARNER', true, 0, false, null, now(), now()),
  ('Mehdi', 'Trabelsi', 'learner.mehdi@training.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'LEARNER', true, 0, false, null, now(), now())
ON CONFLICT (email) DO UPDATE
SET first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    role = EXCLUDED.role,
    enabled = true,
    account_locked = false,
    failed_login_attempts = 0,
    lock_time = null,
    updated_at = now();

-- Trainer profiles.
INSERT INTO trainer_profiles (user_id, phone, bio, cv_url, years_of_experience, average_rating, active, created_at, updated_at)
SELECT u.id, '+216 20 111 001', 'Senior Java and Spring Boot trainer focused on backend architecture and secure APIs.',
       'https://example.com/cv/yassine-trainer.pdf', 8, 4.7, true, now(), now()
FROM users u
WHERE u.email = 'trainer.java@training.com'
  AND NOT EXISTS (SELECT 1 FROM trainer_profiles tp WHERE tp.user_id = u.id);

INSERT INTO trainer_profiles (user_id, phone, bio, cv_url, years_of_experience, average_rating, active, created_at, updated_at)
SELECT u.id, '+216 20 111 002', 'Frontend trainer specialized in Angular, TypeScript, and dashboard interfaces.',
       'https://example.com/cv/ines-frontend.pdf', 6, 4.5, true, now(), now()
FROM users u
WHERE u.email = 'trainer.angular@training.com'
  AND NOT EXISTS (SELECT 1 FROM trainer_profiles tp WHERE tp.user_id = u.id);

INSERT INTO trainer_profiles (user_id, phone, bio, cv_url, years_of_experience, average_rating, active, created_at, updated_at)
SELECT u.id, '+216 20 111 003', 'Management coach for project planning, agile delivery, and business communication.',
       'https://example.com/cv/karim-coach.pdf', 10, 4.8, true, now(), now()
FROM users u
WHERE u.email = 'trainer.management@training.com'
  AND NOT EXISTS (SELECT 1 FROM trainer_profiles tp WHERE tp.user_id = u.id);

INSERT INTO trainer_profile_expertise (trainer_profile_id, skill_id)
SELECT tp.id, s.id
FROM trainer_profiles tp
JOIN users u ON u.id = tp.user_id
JOIN skills s ON s.name IN ('Java', 'Spring Boot', 'Spring Security', 'REST APIs', 'PostgreSQL')
WHERE u.email = 'trainer.java@training.com'
  AND NOT EXISTS (
    SELECT 1 FROM trainer_profile_expertise e WHERE e.trainer_profile_id = tp.id AND e.skill_id = s.id
  );

INSERT INTO trainer_profile_expertise (trainer_profile_id, skill_id)
SELECT tp.id, s.id
FROM trainer_profiles tp
JOIN users u ON u.id = tp.user_id
JOIN skills s ON s.name IN ('Angular', 'TypeScript', 'Communication')
WHERE u.email = 'trainer.angular@training.com'
  AND NOT EXISTS (
    SELECT 1 FROM trainer_profile_expertise e WHERE e.trainer_profile_id = tp.id AND e.skill_id = s.id
  );

INSERT INTO trainer_profile_expertise (trainer_profile_id, skill_id)
SELECT tp.id, s.id
FROM trainer_profiles tp
JOIN users u ON u.id = tp.user_id
JOIN skills s ON s.name IN ('Project Management', 'Agile', 'Communication', 'Business English', 'French')
WHERE u.email = 'trainer.management@training.com'
  AND NOT EXISTS (
    SELECT 1 FROM trainer_profile_expertise e WHERE e.trainer_profile_id = tp.id AND e.skill_id = s.id
  );

-- Trainer availability.
INSERT INTO trainer_availability (trainer_profile_id, day_of_week, start_time, end_time, created_at, updated_at)
SELECT tp.id, data.day_of_week, data.start_time::time, data.end_time::time, now(), now()
FROM trainer_profiles tp
JOIN users u ON u.id = tp.user_id
JOIN (
  VALUES
    ('trainer.java@training.com', 'MONDAY', '09:00', '12:00'),
    ('trainer.java@training.com', 'WEDNESDAY', '14:00', '17:00'),
    ('trainer.angular@training.com', 'TUESDAY', '09:00', '12:00'),
    ('trainer.angular@training.com', 'THURSDAY', '13:00', '16:00'),
    ('trainer.management@training.com', 'MONDAY', '13:00', '16:00'),
    ('trainer.management@training.com', 'FRIDAY', '09:00', '12:00')
) AS data(email, day_of_week, start_time, end_time) ON data.email = u.email
WHERE NOT EXISTS (
  SELECT 1 FROM trainer_availability ta
  WHERE ta.trainer_profile_id = tp.id
    AND ta.day_of_week = data.day_of_week
    AND ta.start_time = data.start_time::time
);

-- Learner profiles.
INSERT INTO learner_profiles (user_id, phone, bio, current_level, learning_goals, created_at, updated_at)
SELECT u.id, '+216 55 222 001', 'Junior developer learning backend development.',
       'BEGINNER', 'spring backend java api', now(), now()
FROM users u
WHERE u.email = 'learner.amine@training.com'
  AND NOT EXISTS (SELECT 1 FROM learner_profiles lp WHERE lp.user_id = u.id);

INSERT INTO learner_profiles (user_id, phone, bio, current_level, learning_goals, created_at, updated_at)
SELECT u.id, '+216 55 222 002', 'Frontend learner preparing for Angular projects.',
       'BEGINNER', 'angular frontend typescript', now(), now()
FROM users u
WHERE u.email = 'learner.sara@training.com'
  AND NOT EXISTS (SELECT 1 FROM learner_profiles lp WHERE lp.user_id = u.id);

INSERT INTO learner_profiles (user_id, phone, bio, current_level, learning_goals, created_at, updated_at)
SELECT u.id, '+216 55 222 003', 'Team lead improving project management and communication.',
       'INTERMEDIATE', 'project management communication agile', now(), now()
FROM users u
WHERE u.email = 'learner.nour@training.com'
  AND NOT EXISTS (SELECT 1 FROM learner_profiles lp WHERE lp.user_id = u.id);

INSERT INTO learner_profiles (user_id, phone, bio, current_level, learning_goals, created_at, updated_at)
SELECT u.id, '+216 55 222 004', 'Software learner interested in full stack web platforms.',
       'INTERMEDIATE', 'full stack java angular', now(), now()
FROM users u
WHERE u.email = 'learner.mehdi@training.com'
  AND NOT EXISTS (SELECT 1 FROM learner_profiles lp WHERE lp.user_id = u.id);

INSERT INTO learner_profile_skills (learner_profile_id, skill_id)
SELECT lp.id, s.id
FROM learner_profiles lp
JOIN users u ON u.id = lp.user_id
JOIN skills s ON s.name IN ('Java', 'REST APIs')
WHERE u.email = 'learner.amine@training.com'
  AND NOT EXISTS (
    SELECT 1 FROM learner_profile_skills lps WHERE lps.learner_profile_id = lp.id AND lps.skill_id = s.id
  );

INSERT INTO learner_profile_skills (learner_profile_id, skill_id)
SELECT lp.id, s.id
FROM learner_profiles lp
JOIN users u ON u.id = lp.user_id
JOIN skills s ON s.name IN ('Angular', 'TypeScript')
WHERE u.email = 'learner.sara@training.com'
  AND NOT EXISTS (
    SELECT 1 FROM learner_profile_skills lps WHERE lps.learner_profile_id = lp.id AND lps.skill_id = s.id
  );

INSERT INTO learner_profile_skills (learner_profile_id, skill_id)
SELECT lp.id, s.id
FROM learner_profiles lp
JOIN users u ON u.id = lp.user_id
JOIN skills s ON s.name IN ('Project Management', 'Communication')
WHERE u.email = 'learner.nour@training.com'
  AND NOT EXISTS (
    SELECT 1 FROM learner_profile_skills lps WHERE lps.learner_profile_id = lp.id AND lps.skill_id = s.id
  );

INSERT INTO learner_profile_skills (learner_profile_id, skill_id)
SELECT lp.id, s.id
FROM learner_profiles lp
JOIN users u ON u.id = lp.user_id
JOIN skills s ON s.name IN ('Java', 'Angular')
WHERE u.email = 'learner.mehdi@training.com'
  AND NOT EXISTS (
    SELECT 1 FROM learner_profile_skills lps WHERE lps.learner_profile_id = lp.id AND lps.skill_id = s.id
  );

-- Training sessions.
INSERT INTO training_sessions (
  formation_id, trainer_profile_id, title, description, start_date, end_date,
  capacity, location, online, meeting_url, status, created_at, updated_at
)
SELECT f.id, tp.id, 'Spring Boot Fundamentals - July Online',
       'Live online Spring Boot fundamentals session with practical API labs.',
       now() + interval '7 days', now() + interval '10 days',
       18, null, true, 'https://meet.example.com/spring-july', 'OPEN', now(), now()
FROM formations f
JOIN trainer_profiles tp ON tp.user_id = (SELECT id FROM users WHERE email = 'trainer.java@training.com')
WHERE f.title = 'Spring Boot Fundamentals'
  AND NOT EXISTS (SELECT 1 FROM training_sessions ts WHERE ts.title = 'Spring Boot Fundamentals - July Online');

INSERT INTO training_sessions (
  formation_id, trainer_profile_id, title, description, start_date, end_date,
  capacity, location, online, meeting_url, status, created_at, updated_at
)
SELECT f.id, tp.id, 'Advanced Spring Security - August Lab',
       'Security-focused backend lab for JWT, roles, password reset, and account locking.',
       now() + interval '20 days', now() + interval '24 days',
       12, 'Training Center Room B', false, null, 'PLANNED', now(), now()
FROM formations f
JOIN trainer_profiles tp ON tp.user_id = (SELECT id FROM users WHERE email = 'trainer.java@training.com')
WHERE f.title = 'Advanced Spring Security'
  AND NOT EXISTS (SELECT 1 FROM training_sessions ts WHERE ts.title = 'Advanced Spring Security - August Lab');

INSERT INTO training_sessions (
  formation_id, trainer_profile_id, title, description, start_date, end_date,
  capacity, location, online, meeting_url, status, created_at, updated_at
)
SELECT f.id, tp.id, 'Angular Essentials - July Onsite',
       'Onsite Angular training with dashboards, guards, forms, and API services.',
       now() + interval '12 days', now() + interval '15 days',
       15, 'Training Center Room A', false, null, 'OPEN', now(), now()
FROM formations f
JOIN trainer_profiles tp ON tp.user_id = (SELECT id FROM users WHERE email = 'trainer.angular@training.com')
WHERE f.title = 'Angular Essentials'
  AND NOT EXISTS (SELECT 1 FROM training_sessions ts WHERE ts.title = 'Angular Essentials - July Onsite');

INSERT INTO training_sessions (
  formation_id, trainer_profile_id, title, description, start_date, end_date,
  capacity, location, online, meeting_url, status, created_at, updated_at
)
SELECT f.id, tp.id, 'Full Stack Java Angular - September Bootcamp',
       'Full stack bootcamp connecting Spring Boot APIs with Angular frontend workflows.',
       now() + interval '35 days', now() + interval '42 days',
       20, null, true, 'https://meet.example.com/fullstack-september', 'PLANNED', now(), now()
FROM formations f
JOIN trainer_profiles tp ON tp.user_id = (SELECT id FROM users WHERE email = 'trainer.angular@training.com')
WHERE f.title = 'Full Stack Java Angular'
  AND NOT EXISTS (SELECT 1 FROM training_sessions ts WHERE ts.title = 'Full Stack Java Angular - September Bootcamp');

INSERT INTO training_sessions (
  formation_id, trainer_profile_id, title, description, start_date, end_date,
  capacity, location, online, meeting_url, status, created_at, updated_at
)
SELECT f.id, tp.id, 'Project Management Professional - Online Cohort',
       'Project management cohort with agile planning and stakeholder communication.',
       now() + interval '9 days', now() + interval '13 days',
       16, null, true, 'https://meet.example.com/project-management', 'OPEN', now(), now()
FROM formations f
JOIN trainer_profiles tp ON tp.user_id = (SELECT id FROM users WHERE email = 'trainer.management@training.com')
WHERE f.title = 'Project Management Professional'
  AND NOT EXISTS (SELECT 1 FROM training_sessions ts WHERE ts.title = 'Project Management Professional - Online Cohort');

INSERT INTO training_sessions (
  formation_id, trainer_profile_id, title, description, start_date, end_date,
  capacity, location, online, meeting_url, status, created_at, updated_at
)
SELECT f.id, tp.id, 'Business Communication - Workshop',
       'Practical workshop for writing, presentations, and workplace conversations.',
       now() + interval '18 days', now() + interval '20 days',
       14, 'Training Center Room C', false, null, 'OPEN', now(), now()
FROM formations f
JOIN trainer_profiles tp ON tp.user_id = (SELECT id FROM users WHERE email = 'trainer.management@training.com')
WHERE f.title = 'Business Communication'
  AND NOT EXISTS (SELECT 1 FROM training_sessions ts WHERE ts.title = 'Business Communication - Workshop');

-- Enrollments.
INSERT INTO enrollments (learner_profile_id, session_id, status, enrolled_at, created_at, updated_at)
SELECT lp.id, ts.id, data.status, now() - interval '2 days', now(), now()
FROM (
  VALUES
    ('learner.amine@training.com', 'Spring Boot Fundamentals - July Online', 'CONFIRMED'),
    ('learner.mehdi@training.com', 'Spring Boot Fundamentals - July Online', 'CONFIRMED'),
    ('learner.sara@training.com', 'Angular Essentials - July Onsite', 'CONFIRMED'),
    ('learner.nour@training.com', 'Project Management Professional - Online Cohort', 'CONFIRMED'),
    ('learner.nour@training.com', 'Business Communication - Workshop', 'PENDING'),
    ('learner.mehdi@training.com', 'Full Stack Java Angular - September Bootcamp', 'CONFIRMED')
) AS data(email, session_title, status)
JOIN users u ON u.email = data.email
JOIN learner_profiles lp ON lp.user_id = u.id
JOIN training_sessions ts ON ts.title = data.session_title
WHERE NOT EXISTS (
  SELECT 1 FROM enrollments e WHERE e.learner_profile_id = lp.id AND e.session_id = ts.id
);

COMMIT;

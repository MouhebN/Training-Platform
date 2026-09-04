package com.training.platform.common.config;

import com.training.platform.catalogue.category.entity.Category;
import com.training.platform.catalogue.category.repository.CategoryRepository;
import com.training.platform.catalogue.chapter.entity.Chapter;
import com.training.platform.catalogue.chapter.repository.ChapterRepository;
import com.training.platform.catalogue.formation.entity.Formation;
import com.training.platform.catalogue.formation.entity.FormationLevel;
import com.training.platform.catalogue.formation.repository.FormationRepository;
import com.training.platform.enrollment.entity.Enrollment;
import com.training.platform.enrollment.entity.EnrollmentStatus;
import com.training.platform.enrollment.repository.EnrollmentRepository;
import com.training.platform.learner.entity.LearnerLevel;
import com.training.platform.learner.entity.LearnerProfile;
import com.training.platform.learner.repository.LearnerProfileRepository;
import com.training.platform.session.entity.SessionStatus;
import com.training.platform.session.entity.TrainingSession;
import com.training.platform.session.repository.TrainingSessionRepository;
import com.training.platform.skill.entity.Skill;
import com.training.platform.skill.repository.SkillRepository;
import com.training.platform.trainer.entity.TrainerAvailability;
import com.training.platform.trainer.entity.TrainerProfile;
import com.training.platform.trainer.repository.TrainerAvailabilityRepository;
import com.training.platform.trainer.repository.TrainerProfileRepository;
import com.training.platform.user.entity.Role;
import com.training.platform.user.entity.User;
import com.training.platform.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEMO_PASSWORD = "password";

    @Bean
    CommandLineRunner seedData(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            CategoryRepository categoryRepository,
            FormationRepository formationRepository,
            ChapterRepository chapterRepository,
            SkillRepository skillRepository,
            TrainerProfileRepository trainerProfileRepository,
            TrainerAvailabilityRepository trainerAvailabilityRepository,
            LearnerProfileRepository learnerProfileRepository,
            TrainingSessionRepository trainingSessionRepository,
            EnrollmentRepository enrollmentRepository
    ) {
        return args -> {
            ensureAdmin(userRepository, passwordEncoder);
            Map<String, Skill> skills = ensureSkills(skillRepository);
            Map<String, Category> categories = ensureCategories(categoryRepository);
            Map<String, Formation> formations = ensureFormations(formationRepository, categories, skills);
            ensureChapters(chapterRepository, formations);

            if (!userRepository.existsByEmail("trainer.java@training.com")) {
                log.info("Seeding demo trainers, learners, availability, sessions, and enrollments");
                Map<String, TrainerProfile> trainers = seedTrainers(
                        userRepository, passwordEncoder, trainerProfileRepository, skills
                );
                seedAvailability(trainerAvailabilityRepository, trainers);
                Map<String, LearnerProfile> learners = seedLearners(
                        userRepository, passwordEncoder, learnerProfileRepository, skills
                );
                if (trainingSessionRepository.count() == 0) {
                    Map<String, TrainingSession> sessions = seedSessions(
                            trainingSessionRepository, formations, trainers
                    );
                    seedEnrollments(enrollmentRepository, learners, sessions);
                }
            } else {
                log.info("Demo users already present — skipping trainer/learner seed");
            }
        };
    }

    private void ensureAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        if (userRepository.existsByEmail("admin@training.com")) {
            return;
        }
        userRepository.save(User.builder()
                .firstName("Platform")
                .lastName("Admin")
                .email("admin@training.com")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .enabled(true)
                .build());
    }

    private Map<String, Skill> ensureSkills(SkillRepository skillRepository) {
        List.of(
                entry("Java", "Java programming language"),
                entry("Spring Boot", "Spring Boot backend development"),
                entry("Spring Security", "Authentication, authorization, and JWT security"),
                entry("REST APIs", "RESTful API design and implementation"),
                entry("PostgreSQL", "Relational database design and SQL with PostgreSQL"),
                entry("Angular", "Angular frontend development"),
                entry("TypeScript", "Typed JavaScript for frontend applications"),
                entry("Project Management", "Planning and managing projects"),
                entry("Agile", "Agile delivery, Scrum, and iterative planning"),
                entry("Communication", "Professional communication skills"),
                entry("Business English", "English for professional environments"),
                entry("French", "French language skills")
        ).forEach(pair -> {
            if (!skillRepository.existsByNameIgnoreCase(pair[0])) {
                skillRepository.save(Skill.builder().name(pair[0]).description(pair[1]).build());
            }
        });
        java.util.HashMap<String, Skill> map = new java.util.HashMap<>();
        skillRepository.findAll().forEach(skill -> map.put(skill.getName(), skill));
        return map;
    }

    private Map<String, Category> ensureCategories(CategoryRepository categoryRepository) {
        List.of(
                entry("IT", "Information technology, software development, and databases"),
                entry("Management", "Leadership, project management, and business skills"),
                entry("Languages", "Professional language learning"),
                entry("Cybersecurity", "Security, identity, access control, and risk awareness")
        ).forEach(pair -> {
            if (!categoryRepository.existsByNameIgnoreCase(pair[0])) {
                categoryRepository.save(Category.builder().name(pair[0]).description(pair[1]).build());
            }
        });
        java.util.HashMap<String, Category> map = new java.util.HashMap<>();
        categoryRepository.findAll().forEach(category -> map.put(category.getName(), category));
        return map;
    }

    private Map<String, Formation> ensureFormations(
            FormationRepository formationRepository,
            Map<String, Category> categories,
            Map<String, Skill> skills
    ) {
        seedFormation(formationRepository, categories, skills,
                "Spring Boot Fundamentals",
                "Build professional REST APIs with Spring Boot, Spring Data JPA, validation, PostgreSQL, and JWT basics.",
                250, FormationLevel.BEGINNER, 24, 2, "IT",
                List.of("Java", "REST APIs", "PostgreSQL"));
        seedFormation(formationRepository, categories, skills,
                "Advanced Spring Security",
                "Secure APIs with JWT, role-based access control, account locking, and password management flows.",
                340, FormationLevel.ADVANCED, 30, 2, "Cybersecurity",
                List.of("Java", "Spring Boot", "Spring Security", "REST APIs"));
        seedFormation(formationRepository, categories, skills,
                "Angular Essentials",
                "Create modern Angular applications with routing, reactive forms, services, guards, and clean UI structure.",
                220, FormationLevel.BEGINNER, 20, 2, "IT",
                List.of("Angular", "TypeScript"));
        seedFormation(formationRepository, categories, skills,
                "Full Stack Java Angular",
                "Connect Spring Boot APIs with an Angular frontend and build a complete professional training platform workflow.",
                420, FormationLevel.INTERMEDIATE, 42, 3, "IT",
                List.of("Java", "Spring Boot", "Angular", "TypeScript", "REST APIs"));
        seedFormation(formationRepository, categories, skills,
                "Project Management Professional",
                "Plan projects, manage risks, follow progress, and communicate effectively with stakeholders.",
                300, FormationLevel.INTERMEDIATE, 28, 2, "Management",
                List.of("Project Management", "Agile", "Communication"));
        seedFormation(formationRepository, categories, skills,
                "Business Communication",
                "Improve workplace communication, presentations, professional writing, and negotiation skills.",
                180, FormationLevel.INTERMEDIATE, 16, 1, "Management",
                List.of("Communication", "Business English"));
        seedFormation(formationRepository, categories, skills,
                "Professional English for IT",
                "Practice technical English for meetings, documentation, interviews, and daily collaboration.",
                160, FormationLevel.BEGINNER, 18, 1, "Languages",
                List.of("Business English", "Communication"));
        seedFormation(formationRepository, categories, skills,
                "French Workplace Communication",
                "Develop French communication skills for business conversations and professional writing.",
                150, FormationLevel.BEGINNER, 18, 1, "Languages",
                List.of("French", "Communication"));

        java.util.HashMap<String, Formation> map = new java.util.HashMap<>();
        formationRepository.findAll().forEach(formation -> map.put(formation.getTitle(), formation));
        return map;
    }

    private void seedFormation(
            FormationRepository formationRepository,
            Map<String, Category> categories,
            Map<String, Skill> skills,
            String title,
            String description,
            int price,
            FormationLevel level,
            int durationHours,
            int sessionCount,
            String categoryName,
            List<String> requiredSkillNames
    ) {
        if (formationRepository.findByTitleIgnoreCase(title).isPresent()) {
            return;
        }
        Set<Skill> required = new HashSet<>();
        requiredSkillNames.forEach(name -> {
            Skill skill = skills.get(name);
            if (skill != null) {
                required.add(skill);
            }
        });
        formationRepository.save(Formation.builder()
                .title(title)
                .description(description)
                .price(BigDecimal.valueOf(price))
                .level(level)
                .durationHours(durationHours)
                .sessionCount(sessionCount)
                .active(true)
                .category(categories.get(categoryName))
                .requiredSkills(required)
                .build());
    }

    private void ensureChapters(ChapterRepository chapterRepository, Map<String, Formation> formations) {
        if (chapterRepository.count() > 0) {
            return;
        }
        record ChapterSeed(String formationTitle, int order, String title, String content) {}
        List.of(
                new ChapterSeed("Spring Boot Fundamentals", 1, "Project setup and architecture", "Create a clean Spring Boot project and understand layered responsibilities."),
                new ChapterSeed("Spring Boot Fundamentals", 2, "REST controllers and validation", "Expose DTO-based endpoints and validate incoming requests."),
                new ChapterSeed("Spring Boot Fundamentals", 3, "JPA and PostgreSQL", "Persist entities and query data with Spring Data JPA."),
                new ChapterSeed("Advanced Spring Security", 1, "JWT authentication flow", "Understand token generation, validation, and stateless sessions."),
                new ChapterSeed("Advanced Spring Security", 2, "Role-based authorization", "Protect endpoints with roles and ownership checks."),
                new ChapterSeed("Advanced Spring Security", 3, "Account protection", "Handle failed login attempts, locking, and password reset flows."),
                new ChapterSeed("Angular Essentials", 1, "Routing and layouts", "Build feature routes and dashboard layouts."),
                new ChapterSeed("Angular Essentials", 2, "Reactive forms", "Create robust forms for login, register, and CRUD pages."),
                new ChapterSeed("Angular Essentials", 3, "HTTP services and guards", "Connect Angular services to secured backend APIs."),
                new ChapterSeed("Full Stack Java Angular", 1, "API integration", "Connect Angular pages to Spring Boot endpoints."),
                new ChapterSeed("Full Stack Java Angular", 2, "JWT in frontend", "Store tokens, attach authorization headers, and guard routes."),
                new ChapterSeed("Project Management Professional", 1, "Project planning", "Define scope, milestones, and delivery risks."),
                new ChapterSeed("Project Management Professional", 2, "Agile execution", "Run iterations and track team progress."),
                new ChapterSeed("Business Communication", 1, "Professional writing", "Write clear emails, summaries, and reports."),
                new ChapterSeed("Business Communication", 2, "Presentations", "Prepare and deliver structured presentations."),
                new ChapterSeed("Professional English for IT", 1, "Technical vocabulary", "Use common software and project vocabulary in English."),
                new ChapterSeed("French Workplace Communication", 1, "Business conversations", "Practice common workplace situations in French.")
        ).forEach(seed -> {
            Formation formation = formations.get(seed.formationTitle());
            if (formation == null) {
                return;
            }
            chapterRepository.save(Chapter.builder()
                    .formation(formation)
                    .orderIndex(seed.order())
                    .title(seed.title())
                    .content(seed.content())
                    .build());
        });
    }

    private Map<String, TrainerProfile> seedTrainers(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TrainerProfileRepository trainerProfileRepository,
            Map<String, Skill> skills
    ) {
        TrainerProfile java = saveTrainer(userRepository, passwordEncoder, trainerProfileRepository,
                "Yassine", "Trainer", "trainer.java@training.com", "+216 20 111 001",
                "Senior Java and Spring Boot trainer focused on backend architecture and secure APIs.",
                8, 4.7, List.of("Java", "Spring Boot", "Spring Security", "REST APIs", "PostgreSQL"), skills);
        TrainerProfile angular = saveTrainer(userRepository, passwordEncoder, trainerProfileRepository,
                "Ines", "Frontend", "trainer.angular@training.com", "+216 20 111 002",
                "Frontend trainer specialized in Angular, TypeScript, and dashboard interfaces.",
                6, 4.5, List.of("Angular", "TypeScript", "Communication"), skills);
        TrainerProfile management = saveTrainer(userRepository, passwordEncoder, trainerProfileRepository,
                "Karim", "Coach", "trainer.management@training.com", "+216 20 111 003",
                "Management coach for project planning, agile delivery, and business communication.",
                10, 4.8, List.of("Project Management", "Agile", "Communication", "Business English", "French"), skills);
        return Map.of(
                "trainer.java@training.com", java,
                "trainer.angular@training.com", angular,
                "trainer.management@training.com", management
        );
    }

    private TrainerProfile saveTrainer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TrainerProfileRepository trainerProfileRepository,
            String firstName,
            String lastName,
            String email,
            String phone,
            String bio,
            int years,
            double rating,
            List<String> expertiseNames,
            Map<String, Skill> skills
    ) {
        User user = userRepository.save(User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .password(passwordEncoder.encode(DEMO_PASSWORD))
                .role(Role.TRAINER)
                .enabled(true)
                .build());
        Set<Skill> expertise = new HashSet<>();
        expertiseNames.forEach(name -> {
            Skill skill = skills.get(name);
            if (skill != null) {
                expertise.add(skill);
            }
        });
        return trainerProfileRepository.save(TrainerProfile.builder()
                .user(user)
                .phone(phone)
                .bio(bio)
                .yearsOfExperience(years)
                .averageRating(rating)
                .expertise(expertise)
                .active(true)
                .build());
    }

    private void seedAvailability(
            TrainerAvailabilityRepository availabilityRepository,
            Map<String, TrainerProfile> trainers
    ) {
        saveSlot(availabilityRepository, trainers.get("trainer.java@training.com"), DayOfWeek.MONDAY, "09:00", "17:00");
        saveSlot(availabilityRepository, trainers.get("trainer.java@training.com"), DayOfWeek.WEDNESDAY, "09:00", "17:00");
        saveSlot(availabilityRepository, trainers.get("trainer.angular@training.com"), DayOfWeek.TUESDAY, "09:00", "17:00");
        saveSlot(availabilityRepository, trainers.get("trainer.angular@training.com"), DayOfWeek.THURSDAY, "09:00", "17:00");
        saveSlot(availabilityRepository, trainers.get("trainer.management@training.com"), DayOfWeek.MONDAY, "13:00", "17:00");
        saveSlot(availabilityRepository, trainers.get("trainer.management@training.com"), DayOfWeek.FRIDAY, "09:00", "17:00");
    }

    private void saveSlot(
            TrainerAvailabilityRepository availabilityRepository,
            TrainerProfile trainer,
            DayOfWeek day,
            String start,
            String end
    ) {
        if (trainer == null) {
            return;
        }
        availabilityRepository.save(TrainerAvailability.builder()
                .trainer(trainer)
                .dayOfWeek(day)
                .startTime(LocalTime.parse(start))
                .endTime(LocalTime.parse(end))
                .build());
    }

    private Map<String, LearnerProfile> seedLearners(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            LearnerProfileRepository learnerProfileRepository,
            Map<String, Skill> skills
    ) {
        LearnerProfile amine = saveLearner(userRepository, passwordEncoder, learnerProfileRepository,
                "Amine", "Ben Ali", "learner.amine@training.com", "+216 55 222 001",
                "Junior developer learning backend development.", LearnerLevel.BEGINNER,
                "spring backend java api", List.of("Java", "REST APIs"), skills);
        LearnerProfile sara = saveLearner(userRepository, passwordEncoder, learnerProfileRepository,
                "Sara", "Mansouri", "learner.sara@training.com", "+216 55 222 002",
                "Frontend learner preparing for Angular projects.", LearnerLevel.BEGINNER,
                "angular frontend typescript", List.of("Angular", "TypeScript"), skills);
        LearnerProfile nour = saveLearner(userRepository, passwordEncoder, learnerProfileRepository,
                "Nour", "Haddad", "learner.nour@training.com", "+216 55 222 003",
                "Team lead improving project management and communication.", LearnerLevel.INTERMEDIATE,
                "project management communication agile", List.of("Project Management", "Communication"), skills);
        LearnerProfile mehdi = saveLearner(userRepository, passwordEncoder, learnerProfileRepository,
                "Mehdi", "Trabelsi", "learner.mehdi@training.com", "+216 55 222 004",
                "Software learner interested in full stack web platforms.", LearnerLevel.INTERMEDIATE,
                "full stack java angular", List.of("Java", "Angular"), skills);
        return Map.of(
                "learner.amine@training.com", amine,
                "learner.sara@training.com", sara,
                "learner.nour@training.com", nour,
                "learner.mehdi@training.com", mehdi
        );
    }

    private LearnerProfile saveLearner(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            LearnerProfileRepository learnerProfileRepository,
            String firstName,
            String lastName,
            String email,
            String phone,
            String bio,
            LearnerLevel level,
            String goals,
            List<String> skillNames,
            Map<String, Skill> skills
    ) {
        User user = userRepository.save(User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .password(passwordEncoder.encode(DEMO_PASSWORD))
                .role(Role.LEARNER)
                .enabled(true)
                .build());
        Set<Skill> owned = new HashSet<>();
        skillNames.forEach(name -> {
            Skill skill = skills.get(name);
            if (skill != null) {
                owned.add(skill);
            }
        });
        return learnerProfileRepository.save(LearnerProfile.builder()
                .user(user)
                .phone(phone)
                .bio(bio)
                .currentLevel(level)
                .learningGoals(goals)
                .skills(owned)
                .build());
    }

    private Map<String, TrainingSession> seedSessions(
            TrainingSessionRepository trainingSessionRepository,
            Map<String, Formation> formations,
            Map<String, TrainerProfile> trainers
    ) {
        TrainingSession spring = saveSession(trainingSessionRepository,
                formations.get("Spring Boot Fundamentals"),
                trainers.get("trainer.java@training.com"),
                "Spring Boot Fundamentals - Session 1",
                "Live online Spring Boot fundamentals session with practical API labs.",
                7, 10, 18, true, null, SessionStatus.OPEN);
        TrainingSession spring2 = saveSession(trainingSessionRepository,
                formations.get("Spring Boot Fundamentals"),
                trainers.get("trainer.java@training.com"),
                "Spring Boot Fundamentals - Session 2",
                "Second séance for Spring Boot fundamentals.",
                21, 24, 18, true, null, SessionStatus.PLANNED);
        TrainingSession security = saveSession(trainingSessionRepository,
                formations.get("Advanced Spring Security"),
                trainers.get("trainer.java@training.com"),
                "Advanced Spring Security - Lab",
                "Security-focused backend lab for JWT, roles, password reset, and account locking.",
                20, 24, 12, false, "Training Center Room B", SessionStatus.PLANNED);
        TrainingSession angular = saveSession(trainingSessionRepository,
                formations.get("Angular Essentials"),
                trainers.get("trainer.angular@training.com"),
                "Angular Essentials - Session 1",
                "Onsite Angular training with dashboards, guards, forms, and API services.",
                12, 15, 15, false, "Training Center Room A", SessionStatus.OPEN);
        TrainingSession fullstack = saveSession(trainingSessionRepository,
                formations.get("Full Stack Java Angular"),
                trainers.get("trainer.angular@training.com"),
                "Full Stack Java Angular - Bootcamp",
                "Full stack bootcamp connecting Spring Boot APIs with Angular frontend workflows.",
                35, 42, 20, true, null, SessionStatus.PLANNED);
        TrainingSession pm = saveSession(trainingSessionRepository,
                formations.get("Project Management Professional"),
                trainers.get("trainer.management@training.com"),
                "Project Management Professional - Cohort",
                "Project management cohort with agile planning and stakeholder communication.",
                9, 13, 16, true, null, SessionStatus.OPEN);
        TrainingSession communication = saveSession(trainingSessionRepository,
                formations.get("Business Communication"),
                trainers.get("trainer.management@training.com"),
                "Business Communication - Workshop",
                "Practical workshop for writing, presentations, and workplace conversations.",
                18, 20, 14, false, "Training Center Room C", SessionStatus.OPEN);

        java.util.HashMap<String, TrainingSession> map = new java.util.HashMap<>();
        if (spring != null) map.put(spring.getTitle(), spring);
        if (spring2 != null) map.put(spring2.getTitle(), spring2);
        if (security != null) map.put(security.getTitle(), security);
        if (angular != null) map.put(angular.getTitle(), angular);
        if (fullstack != null) map.put(fullstack.getTitle(), fullstack);
        if (pm != null) map.put(pm.getTitle(), pm);
        if (communication != null) map.put(communication.getTitle(), communication);
        return map;
    }

    private TrainingSession saveSession(
            TrainingSessionRepository trainingSessionRepository,
            Formation formation,
            TrainerProfile trainer,
            String title,
            String description,
            int startPlusDays,
            int endPlusDays,
            int capacity,
            boolean online,
            String location,
            SessionStatus status
    ) {
        if (formation == null || trainer == null) {
            return null;
        }
        return trainingSessionRepository.save(TrainingSession.builder()
                .formation(formation)
                .trainer(trainer)
                .title(title)
                .description(description)
                .startDate(LocalDateTime.now().plusDays(startPlusDays).withHour(9).withMinute(0).withSecond(0).withNano(0))
                .endDate(LocalDateTime.now().plusDays(endPlusDays).withHour(12).withMinute(0).withSecond(0).withNano(0))
                .capacity(capacity)
                .online(online)
                .location(location)
                .meetingUrl(online ? "https://meet.jit.si/" + title.toLowerCase().replace(' ', '-') : null)
                .status(status)
                .build());
    }

    private void seedEnrollments(
            EnrollmentRepository enrollmentRepository,
            Map<String, LearnerProfile> learners,
            Map<String, TrainingSession> sessions
    ) {
        enroll(enrollmentRepository, learners.get("learner.amine@training.com"),
                sessions.get("Spring Boot Fundamentals - Session 1"), EnrollmentStatus.CONFIRMED);
        enroll(enrollmentRepository, learners.get("learner.mehdi@training.com"),
                sessions.get("Spring Boot Fundamentals - Session 1"), EnrollmentStatus.CONFIRMED);
        enroll(enrollmentRepository, learners.get("learner.sara@training.com"),
                sessions.get("Angular Essentials - Session 1"), EnrollmentStatus.CONFIRMED);
        enroll(enrollmentRepository, learners.get("learner.nour@training.com"),
                sessions.get("Project Management Professional - Cohort"), EnrollmentStatus.CONFIRMED);
        enroll(enrollmentRepository, learners.get("learner.nour@training.com"),
                sessions.get("Business Communication - Workshop"), EnrollmentStatus.WAITLISTED);
        enroll(enrollmentRepository, learners.get("learner.mehdi@training.com"),
                sessions.get("Full Stack Java Angular - Bootcamp"), EnrollmentStatus.CONFIRMED);
    }

    private void enroll(
            EnrollmentRepository enrollmentRepository,
            LearnerProfile learner,
            TrainingSession session,
            EnrollmentStatus status
    ) {
        if (learner == null || session == null) {
            return;
        }
        if (enrollmentRepository.existsByLearnerIdAndSessionId(learner.getId(), session.getId())) {
            return;
        }
        enrollmentRepository.save(Enrollment.builder()
                .learner(learner)
                .session(session)
                .status(status)
                .enrolledAt(LocalDateTime.now().minusDays(2))
                .build());
    }

    private String[] entry(String name, String description) {
        return new String[] {name, description};
    }
}

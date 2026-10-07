package com.deepblue.rescue;

import com.deepblue.rescue.domain.*;
import com.deepblue.rescue.repository.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest
@Transactional
class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("deepblue_test")
            .withUsername("deepblue")
            .withPassword("deepblue");

    @Autowired
    private RescueCenterRepository rescueCenterRepository;

    @Autowired
    private RescueCaseRepository rescueCaseRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private SpecialistRepository specialistRepository;

    @Autowired
    private ExpertiseRepository expertiseRepository;

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayShouldHaveExecutedMigrations() {
        // Consultamos directamente a la base de datos usando SQL nativo
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version IN ('1', '2')",
                Integer.class);

        // Verificamos que encontró exactamente las 2 migraciones (V1 y V2)
        assertThat(count).isEqualTo(2);
    }

    @Test
    void testInheritedMethodsWithRescueCenter() {
        // 1. Crear la entidad con los datos solicitados
        RescueCenter center = new RescueCenter();
        center.setCode("DB-CAR");
        center.setName("DeepBlue Caribbean Center");
        center.setCity("Santa Marta");

        // 2. Probar save()
        RescueCenter savedCenter = rescueCenterRepository.save(center);
        assertThat(savedCenter.getId()).isNotNull(); // Verificamos que PostgreSQL le asignó un ID

        // 3. Probar findById()
        Optional<RescueCenter> foundCenter = rescueCenterRepository.findById(savedCenter.getId());
        assertThat(foundCenter).isPresent();
        assertThat(foundCenter.get().getName()).isEqualTo("DeepBlue Caribbean Center");

        // 4. Probar existsById()
        boolean exists = rescueCenterRepository.existsById(savedCenter.getId());
        assertThat(exists).isTrue();

        // 5. Probar count()
        long count = rescueCenterRepository.count();
        assertThat(count).isGreaterThanOrEqualTo(1L); // Verificamos que hay al menos 1 centro en la DB
    }

    @Test
    void testRescueCenterOneToManyRescueCases() {
        // 1. Crear y persistir el centro
        RescueCenter center = new RescueCenter();
        center.setCode("DB-PAC");
        center.setName("DeepBlue Pacific");
        center.setCity("Buenaventura");

        // Guardamos el centro primero para que tenga un ID asignado
        rescueCenterRepository.save(center);

        // 2. Crear el primer caso de rescate
        RescueCase case1 = new RescueCase();
        case1.setCaseCode("RES-PAC-001");
        case1.setStatus(RescueStatus.ADMITTED);
        case1.setRescueDate(LocalDate.now());
        case1.setRescueLocation("locacion de prueba");
        // Usamos el método de conveniencia que creaste en el Paso 19
        center.addCase(case1);

        // 3. Crear el segundo caso de rescate
        RescueCase case2 = new RescueCase();
        case2.setCaseCode("RES-PAC-002");
        case2.setStatus(RescueStatus.UNDER_EVALUATION);
        case2.setRescueDate(LocalDate.now());
        case2.setRescueLocation("locacion de prueba");
        center.addCase(case2);

        // 4. Persistir los casos
        rescueCaseRepository.saveAll(List.of(case1, case2));

        // 5. Comprobar que ambos casos pertenecen al mismo centro
        RescueCase savedCase1 = rescueCaseRepository.findById(case1.getId()).orElseThrow();
        RescueCase savedCase2 = rescueCaseRepository.findById(case2.getId()).orElseThrow();

        assertThat(savedCase1.getRescueCenter().getId()).isEqualTo(center.getId());
        assertThat(savedCase2.getRescueCenter().getId()).isEqualTo(center.getId());
    }

    @Test
    void testRescueCaseOneToOneAnimal() {
        // 1. Crear un centro requerido para guardar el caso
        RescueCenter center = new RescueCenter();
        center.setCode("DB-TEST-1TO1");
        center.setName("Center for 1:1 Test");
        center.setCity("Test City");
        rescueCenterRepository.save(center);

        // 2. Crear el caso de rescate
        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-2026-001");
        rescueCase.setStatus(RescueStatus.ADMITTED);
        rescueCase.setRescueDate(LocalDate.now());
        rescueCase.setRescueLocation("locacion de prueba");
        center.addCase(rescueCase);

        // 3. Crear el animal
        Animal animal = new Animal();
        animal.setAnimalCode("AN-2026-001");
        animal.setCommonName("Green Sea Turtle");
        animal.setScientificName("Chelonia mydas");
        animal.setSex(AnimalSex.FEMALE); // Aseguramos el enum obligatorio

        // 4. Establecer la relación usando el método de conveniencia (Paso 21)
        rescueCase.assignAnimal(animal);

        // 5. Persistir (Guardar el caso también guarda el animal gracias al
        // CascadeType.ALL)
        rescueCaseRepository.save(rescueCase);

        // 6. Comprobar la bidireccionalidad desde la base de datos
        RescueCase savedCase = rescueCaseRepository.findById(rescueCase.getId()).orElseThrow();
        Animal savedAnimal = savedCase.getAnimal();

        // Verificamos case.getAnimal()
        assertThat(savedAnimal).isNotNull();
        assertThat(savedAnimal.getAnimalCode()).isEqualTo("AN-2026-001");

        // Verificamos animal.getRescueCase()
        assertThat(savedAnimal.getRescueCase().getId()).isEqualTo(savedCase.getId());
    }

    @Test
    void testAnimalOneToOneMedicalRecord1() {
        // 1. Crear dependencias previas (Centro y Caso) para no violar constraints de
        // la DB
        RescueCenter center = new RescueCenter();
        center.setCode("DB-MED-TEST");
        center.setName("Medical Test Center");
        center.setCity("Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-2026-002");
        rescueCase.setStatus(RescueStatus.ADMITTED);
        rescueCase.setRescueDate(LocalDate.now());
        rescueCase.setRescueLocation("locacion de prueba");
        center.addCase(rescueCase);
        rescueCaseRepository.save(rescueCase);

        // 2. Crear el animal solicitado
        Animal animal = new Animal();
        animal.setAnimalCode("AN-2026-002");
        animal.setCommonName("Hawksbill Turtle");
        animal.setScientificName("Eretmochelys imbricata");
        animal.setSex(AnimalSex.MALE); // Aseguramos el enum obligatorio
        rescueCase.assignAnimal(animal);

        // 3. Crear el expediente médico
        MedicalRecord record = new MedicalRecord();
        record.setInitialWeight(new BigDecimal("28.40"));
        record.setInitialCondition("STABLE");
        record.setInjuries("Left front flipper injury");

        // 4. Establecer la relación usando el método de conveniencia (Paso 23)
        animal.assignMedicalRecord(record);

        // 5. Guardar el animal (y gracias al CascadeType.ALL, se guarda el expediente)
        animalRepository.save(animal);

        // 6. Comprobar que ambos objetos obtuvieron un ID de PostgreSQL
        assertThat(animal.getId()).isNotNull();
        assertThat(record.getId()).isNotNull();

        // Verificación extra opcional para confirmar que se guardó correctamente
        Animal savedAnimal = animalRepository.findById(animal.getId()).orElseThrow();
        assertThat(savedAnimal.getMedicalRecord().getInitialWeight()).isEqualByComparingTo("28.40");
    }

    @Test
    public void testAnimalOneToOneMedicalRecord() {
        // 1. Crear y guardar el centro de rescate y el caso de rescate previos
        RescueCenter center = new RescueCenter();
        center.setCode("CTR-51");
        center.setName("Centro Costa Verde");
        center.setCity("Ciénaga");
        RescueCenter savedCenter = rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("CASO-51");
        rescueCase.setRescueCenter(savedCenter);
        rescueCase.setRescueDate(LocalDate.now());
        rescueCase.setRescueLocation("Barrio Costa Verde");
        rescueCase.setStatus(RescueStatus.IN_REHABILITATION);
        RescueCase savedCase = rescueCaseRepository.save(rescueCase);

        // 2. Crear la instancia de Animal y asociarla al caso
        Animal animal = new Animal();
        animal.setAnimalCode("ANI-51");
        animal.setCommonName("Pelícano");
        animal.setScientificName("Pelecanus occidentalis");
        animal.setSex(AnimalSex.FEMALE);
        animal.setRescueCase(savedCase);

        // 3. Crear el expediente médico (MedicalRecord)
        MedicalRecord record = new MedicalRecord();
        record.setInitialCondition("Stable");
        record.setInitialWeight(new BigDecimal(3.5));
        ;
        record.setInjuries("Minor wing abrasion");
        record.setObservations("Rescued near the beach");

        // 4. Vincular mediante el método de conveniencia o asignación directa
        animal.assignMedicalRecord(record); // O animal.setMedicalRecord(record) según tu implementación

        // 5. Guardar el animal (se persiste el expediente en cascada gracias a
        // CascadeType.ALL)
        Animal savedAnimal = animalRepository.save(animal);

        // 6. Aserciones de validación
        assertNotNull(savedAnimal.getId());
        assertNotNull(savedAnimal.getMedicalRecord());
        assertNotNull(savedAnimal.getMedicalRecord().getId());
        assertEquals("Stable", savedAnimal.getMedicalRecord().getInitialCondition());
    }

    @Test
    public void testSpecialistManyToManyExpertise() {
        // 1. Recuperar especialidades existentes del catálogo insertado por Flyway en
        // V2
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma")
                .orElseThrow(() -> new RuntimeException("Expertise not found"));
        Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation")
                .orElseThrow(() -> new RuntimeException("Expertise not found"));

        // 2. Crear un especialista y asociar las especialidades usando el método de
        // conveniencia
        Specialist specialist = new Specialist();
        specialist.setProfessionalCode("SPEC-52");
        specialist.setFirstName("Elena");
        specialist.setLastName("Vargas");
        specialist.setEmail("elena.vargas@deepblue.org");
        specialist.setActive(true);

        specialist.addExpertise(trauma);
        specialist.addExpertise(rehabilitation);

        // 3. Persistir el especialista
        Specialist savedSpecialist = specialistRepository.save(specialist);

        // 4. Aserciones para verificar la relación N:M
        assertNotNull(savedSpecialist.getId());
        assertEquals(2, savedSpecialist.getExpertiseAreas().size());
    }

    @Test
public void testFindByRescueCaseStatus() {
    // 1. Crear un centro de rescate previo
    RescueCenter center = new RescueCenter();
    center.setCode("CTR-53");
    center.setName("Centro Test Status");
    center.setCity("Ciénaga");
    RescueCenter savedCenter = rescueCenterRepository.save(center);

    // 2. Crear casos de rescate usando directamente el enum RescueStatus
    RescueCase case1 = new RescueCase();
    case1.setCaseCode("CASO-53-01");
    case1.setRescueCenter(savedCenter);
    case1.setRescueDate(LocalDate.now().minusDays(2));
    case1.setRescueLocation("Playas de Ciénaga");
    case1.setStatus(RescueStatus.IN_REHABILITATION);
    rescueCaseRepository.save(case1);

    RescueCase case2 = new RescueCase();
    case2.setCaseCode("CASO-53-02");
    case2.setRescueCenter(savedCenter);
    case2.setRescueDate(LocalDate.now().minusDays(1));
    case2.setRescueLocation("Costa Verde");
    case2.setStatus(RescueStatus.READY_FOR_RELEASE);
    rescueCaseRepository.save(case2);

    RescueCase case3 = new RescueCase();
    case3.setCaseCode("CASO-53-03");
    case3.setRescueCenter(savedCenter);
    case3.setRescueDate(LocalDate.now());
    case3.setRescueLocation("Malecón");
    case3.setStatus(RescueStatus.IN_REHABILITATION);
    rescueCaseRepository.save(case3);

    // 3. Ejecutar el Query Method por status
    List<RescueCase> inRehabilitationCases = rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION);

    // 4. Verificar aserciones exactas para asegurar que devuelva los 2 casos esperados
    assertNotNull(inRehabilitationCases);
    assertEquals(2, inRehabilitationCases.size());
}

//lllllllllllll

@Test
public void testSpecialistJpqlByExpertise() {
    // 1. Recuperar áreas de experiencia del catálogo de Flyway (V2)
    Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
    Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();
    Expertise marineMammals = expertiseRepository.findByNameIgnoreCase("Marine Mammals").orElseThrow();
    Expertise marineBirds = expertiseRepository.findByNameIgnoreCase("Marine Birds").orElseThrow();

    // 2. Crear especialistas con sus respectivas experiencias
    Specialist elena = new Specialist();
    elena.setProfessionalCode("SPEC-ELENA");
    elena.setFirstName("Elena");
    elena.setLastName("Vargas");
    elena.setEmail("elena@deepblue.org");
    elena.setActive(true);
    elena.addExpertise(trauma);
    elena.addExpertise(rehabilitation);
    specialistRepository.save(elena);

    Specialist mateo = new Specialist();
    mateo.setProfessionalCode("SPEC-MATEO");
    mateo.setFirstName("Mateo");
    mateo.setLastName("Gomez");
    mateo.setEmail("mateo@deepblue.org");
    mateo.setActive(true);
    mateo.addExpertise(marineMammals);
    mateo.addExpertise(rehabilitation);
    specialistRepository.save(mateo);

    Specialist sofia = new Specialist();
    sofia.setProfessionalCode("SPEC-SOFIA");
    sofia.setFirstName("Sofia");
    sofia.setLastName("Castro");
    sofia.setEmail("sofia@deepblue.org");
    sofia.setActive(true);
    sofia.addExpertise(marineBirds);
    sofia.addExpertise(trauma);
    specialistRepository.save(sofia);

    // 3. Consultar usando la JPQL de especialistas (Paso 55)
    List<Specialist> traumaSpecialists = specialistRepository.findActiveByExpertise("Trauma");
    
    // 4. Verificar que solo retorne a Elena y Sofia
    assertNotNull(traumaSpecialists);
    assertEquals(2, traumaSpecialists.size());
}

@Test
public void testTreatmentQueryMethodChronological() {
    // 1. Crear dependencias previas (Centro, Caso, Animal y Especialistas) (Paso 56)
    RescueCenter center = new RescueCenter();
    center.setCode("CTR-TR");
    center.setName("Center Treatments");
    center.setCity("Santa Marta");
    RescueCenter savedCenter = rescueCenterRepository.save(center);

    RescueCase rescueCase = new RescueCase();
    rescueCase.setCaseCode("CASO-TR");
    rescueCase.setRescueCenter(savedCenter);
    rescueCase.setRescueDate(LocalDate.now());
    rescueCase.setRescueLocation("Beach");
    rescueCase.setStatus(RescueStatus.IN_REHABILITATION);
    RescueCase savedCase = rescueCaseRepository.save(rescueCase);

    Animal animal = new Animal();
    animal.setAnimalCode("ANI-TR");
    animal.setCommonName("Sea Turtle");
    animal.setScientificName("Chelonia mydas");
    animal.setSex(AnimalSex.MALE);
    animal.setRescueCase(savedCase);
    Animal savedAnimal = animalRepository.save(animal);

    Specialist elena = new Specialist();
    elena.setProfessionalCode("SPEC-TR-1");
    elena.setFirstName("Elena");
    elena.setLastName("Vargas");
    elena.setEmail("elena.tr@deepblue.org");
    elena.setActive(true);
    Specialist savedElena = specialistRepository.save(elena);

    Specialist mateo = new Specialist();
    mateo.setProfessionalCode("SPEC-TR-2");
    mateo.setFirstName("Mateo");
    mateo.setLastName("Gomez");
    mateo.setEmail("mateo.tr@deepblue.org");
    mateo.setActive(true);
    Specialist savedMateo = specialistRepository.save(mateo);

    // 2. Registrar los 3 tratamientos con marcas de tiempo cronológicas (Paso 56)
    Treatment t1 = new Treatment();
    t1.setAnimal(savedAnimal);
    t1.setSpecialist(savedElena);
    t1.setType(TreatmentType.WOUND_CARE);
    t1.setDescription("Cleaning of left front flipper");
    t1.setPerformedAt(LocalDateTime.now().minusHours(3));
    treatmentRepository.save(t1);

    Treatment t2 = new Treatment();
    t2.setAnimal(savedAnimal);
    t2.setSpecialist(savedElena);
    t2.setType(TreatmentType.HYDRATION);
    t2.setDescription("Subcutaneous fluid therapy");
    t2.setPerformedAt(LocalDateTime.now().minusHours(2));
    treatmentRepository.save(t2);

    Treatment t3 = new Treatment();
    t3.setAnimal(savedAnimal);
    t3.setSpecialist(savedMateo);
    t3.setType(TreatmentType.OBSERVATION);
    t3.setDescription("General monitoring");
    t3.setPerformedAt(LocalDateTime.now().minusHours(1));
    treatmentRepository.save(t3);

    // 3. Ejecutar el Query Method ordenado cronológicamente (Paso 57)
    List<Treatment> treatments = treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(savedAnimal.getId());

    // 4. Verificar que se obtengan los 3 tratamientos en el orden correcto
    assertNotNull(treatments);
    assertEquals(3, treatments.size());
    assertEquals(TreatmentType.WOUND_CARE, treatments.get(0).getType());
    assertEquals(TreatmentType.HYDRATION, treatments.get(1).getType());
    assertEquals(TreatmentType.OBSERVATION, treatments.get(2).getType());
}

//----

@Test
public void testTreatmentJpqlByInterval() {
    // 1. Crear dependencias previas (Centro, Caso, Animal, Especialista)
    RescueCenter center = new RescueCenter();
    center.setCode("CTR-58");
    center.setName("Center Interval");
    center.setCity("Santa Marta");
    RescueCenter savedCenter = rescueCenterRepository.save(center);

    RescueCase rescueCase = new RescueCase();
    rescueCase.setCaseCode("CASO-58");
    rescueCase.setRescueCenter(savedCenter);
    rescueCase.setRescueDate(LocalDate.of(2026, 8, 1));
    rescueCase.setRescueLocation("Beach");
    rescueCase.setStatus(RescueStatus.IN_REHABILITATION);
    RescueCase savedCase = rescueCaseRepository.save(rescueCase);

    Animal animal = new Animal();
    animal.setAnimalCode("ANI-58");
    animal.setCommonName("Sea Turtle");
    animal.setScientificName("Chelonia mydas");
    animal.setSex(AnimalSex.MALE);
    animal.setRescueCase(savedCase);
    Animal savedAnimal = animalRepository.save(animal);

    Specialist specialist = new Specialist();
    specialist.setProfessionalCode("SPEC-58");
    specialist.setFirstName("Elena");
    specialist.setLastName("Vargas");
    specialist.setEmail("elena.58@deepblue.org");
    specialist.setActive(true);
    Specialist savedSpecialist = specialistRepository.save(specialist);

    // 2. Crear los 3 tratamientos con las fechas exactas requeridas en el paso 58[cite: 1]
    Treatment t1 = new Treatment();
    t1.setAnimal(savedAnimal);
    t1.setSpecialist(savedSpecialist);
    t1.setType(TreatmentType.WOUND_CARE);
    t1.setDescription("Treatment Aug 1");
    t1.setPerformedAt(LocalDateTime.of(2026, 8, 1, 10, 0));
    treatmentRepository.save(t1);

    Treatment t2 = new Treatment();
    t2.setAnimal(savedAnimal);
    t2.setSpecialist(savedSpecialist);
    t2.setType(TreatmentType.HYDRATION);
    t2.setDescription("Treatment Aug 10");
    t2.setPerformedAt(LocalDateTime.of(2026, 8, 10, 10, 0));
    treatmentRepository.save(t2);

    Treatment t3 = new Treatment();
    t3.setAnimal(savedAnimal);
    t3.setSpecialist(savedSpecialist);
    t3.setType(TreatmentType.OBSERVATION);
    t3.setDescription("Treatment Aug 20");
    t3.setPerformedAt(LocalDateTime.of(2026, 8, 20, 10, 0));
    treatmentRepository.save(t3);

    // 3. Consultar entre 2026-08-05 y 2026-08-15[cite: 1]
    LocalDateTime start = LocalDateTime.of(2026, 8, 5, 0, 0);
    LocalDateTime end = LocalDateTime.of(2026, 8, 15, 23, 59);
    List<Treatment> treatmentsInInterval = treatmentRepository.findTreatmentsBetweenDates(start, end);

    // 4. Verificar que regrese únicamente el tratamiento del 2026-08-10[cite: 1]
    assertNotNull(treatmentsInInterval);
    assertEquals(1, treatmentsInInterval.size());
    assertEquals(LocalDateTime.of(2026, 8, 10, 10, 0), treatmentsInInterval.get(0).getPerformedAt());
}

//test unique
@Test
public void testUniqueConstraintViolation() {
    // 1. Crear dependencias previas necesarias para el animal
    RescueCenter center = new RescueCenter();
    center.setCode("CTR-59");
    center.setName("Center Unique");
    center.setCity("Ciénaga");
    RescueCenter savedCenter = rescueCenterRepository.save(center);

    RescueCase rescueCase = new RescueCase();
    rescueCase.setCaseCode("CASO-59");
    rescueCase.setRescueCenter(savedCenter);
    rescueCase.setRescueDate(LocalDate.now());
    rescueCase.setRescueLocation("Costa Verde");
    rescueCase.setStatus(RescueStatus.IN_REHABILITATION);
    RescueCase savedCase = rescueCaseRepository.save(rescueCase);

    // 2. Crear el primer animal con un código único (ej: "AN-100")
    Animal animal1 = new Animal();
    animal1.setAnimalCode("AN-100");
    animal1.setCommonName("Turtle");
    animal1.setScientificName("Chelonia mydas");
    animal1.setSex(AnimalSex.UNKNOWN);
    animal1.setRescueCase(savedCase);
    
    // Guardar y vaciar inmediatamente para forzar el envío del SQL a PostgreSQL
    animalRepository.saveAndFlush(animal1);

    // 3. Crear un segundo animal con exactamente el mismo animalCode ("AN-100")
    Animal animal2 = new Animal();
    animal2.setAnimalCode("AN-100");
    animal2.setCommonName("Another Turtle");
    animal2.setScientificName("Chelonia mydas");
    animal2.setSex(AnimalSex.FEMALE);
    animal2.setRescueCase(savedCase);

    // 4. Verificar que al intentar guardar el segundo con saveAndFlush se lance la excepción de integridad
    assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
        animalRepository.saveAndFlush(animal2);
    });
}

//test de fk

@Test
public void testForeignKeyConstraintViolation() {
    // 1. Crear una instancia de Animal asignándole un ID de caso de rescate inexistente (ej: 99999L)
    Animal invalidAnimal = new Animal();
    invalidAnimal.setAnimalCode("AN-FK-ERR");
    invalidAnimal.setCommonName("Test Animal");
    invalidAnimal.setScientificName("Test species");
    invalidAnimal.setSex(AnimalSex.UNKNOWN);

    // Creamos un RescueCase fantasma solo con un ID que no existe en la base de datos
    RescueCase nonExistentCase = new RescueCase();
    nonExistentCase.setId(99999L);
    invalidAnimal.setRescueCase(nonExistentCase);

    // 2. Verificar que al intentar guardar y vaciar con saveAndFlush se lance la DataIntegrityViolationException por la FK
    assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
        animalRepository.saveAndFlush(invalidAnimal);
    });
}
 
//test check

@Test
public void testCheckConstraintViolation() {
    // 1. Crear y guardar un centro de rescate previo usando el repositorio
    RescueCenter center = new RescueCenter();
    center.setCode("CTR-61");
    center.setName("Center Check");
    center.setCity("Ciénaga");
    RescueCenter savedCenter = rescueCenterRepository.saveAndFlush(center);

    // 2. Intentar insertar directamente con SQL nativo un estado que NO existe en el CHECK de PostgreSQL (ej: "ESTADO_INVALIDO")
    String sql = "INSERT INTO rescue_cases (case_code, rescue_date, rescue_location, status, rescue_center_id) VALUES (?, CURRENT_DATE, ?, ?, ?)";

    // 3. Verificar que PostgreSQL rechace la inserción lanzando la DataIntegrityViolationException
    assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
        jdbcTemplate.update(sql, "CASO-CHECK-ERR", "Playa", "ESTADO_INVALIDO", savedCenter.getId());
    });
}

//reto integrador

@Test
public void testRetoIntegradorCompleto() {
    // 1. Centro de Rescate (DB-CAR)[cite: 1]
    RescueCenter center = new RescueCenter();
    center.setCode("DB-CAR");
    center.setName("DeepBlue Caribbean");
    center.setCity("Santa Marta");
    RescueCenter savedCenter = rescueCenterRepository.save(center);

    // 2. Caso de Rescate (RES-2026-100)[cite: 1]
    RescueCase rescueCase = new RescueCase();
    rescueCase.setCaseCode("RES-2026-100");
    rescueCase.setRescueDate(LocalDate.of(2026, 8, 18));
    rescueCase.setRescueLocation("Bahía Concha");
    rescueCase.setStatus(RescueStatus.IN_REHABILITATION);
    rescueCase.setRescueCenter(savedCenter);
    RescueCase savedCase = rescueCaseRepository.save(rescueCase);

    // 3. Animal (AN-2026-100) y Expediente Médico[cite: 1]
    Animal animal = new Animal();
    animal.setAnimalCode("AN-2026-100");
    animal.setCommonName("Green Sea Turtle");
    animal.setScientificName("Chelonia mydas");
    animal.setSex(AnimalSex.FEMALE);
    savedCase.assignAnimal(animal);

    MedicalRecord medicalRecord = new MedicalRecord();
    medicalRecord.setInitialWeight(new java.math.BigDecimal("27.80"));
    medicalRecord.setInitialCondition("STABLE");
    medicalRecord.setInjuries("Injury caused by fishing net");
    medicalRecord.setObservations("Possible plastic ingestion");
    animal.assignMedicalRecord(medicalRecord);
    Animal savedAnimal = animalRepository.save(animal);

    // 4. Especialista (SPEC-001 - Elena Vargas) y Experiencias[cite: 1]
    Expertise marineReptiles = expertiseRepository.findByNameIgnoreCase("Marine Reptiles").orElseThrow();
    Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
    Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();

    Specialist specialist = new Specialist();
    specialist.setProfessionalCode("SPEC-001");
    specialist.setFirstName("Elena");
    specialist.setLastName("Vargas");
    specialist.setEmail("elena@deepblue.org");
    specialist.setActive(true);
    specialist.addExpertise(marineReptiles);
    specialist.addExpertise(trauma);
    specialist.addExpertise(rehabilitation);
    Specialist savedSpecialist = specialistRepository.save(specialist);

    // 5. Tratamientos[cite: 1]
    Treatment t1 = new Treatment();
    t1.setAnimal(savedAnimal);
    t1.setSpecialist(savedSpecialist);
    t1.setPerformedAt(LocalDateTime.of(2026, 8, 18, 10, 0));
    t1.setType(TreatmentType.WOUND_CARE);
    t1.setDescription("Cleaning of left front flipper");
    treatmentRepository.save(t1);

    Treatment t2 = new Treatment();
    t2.setAnimal(savedAnimal);
    t2.setSpecialist(savedSpecialist);
    t2.setPerformedAt(LocalDateTime.of(2026, 8, 18, 11, 0));
    t2.setType(TreatmentType.HYDRATION);
    t2.setDescription("Subcutaneous fluid therapy");
    treatmentRepository.save(t2);

    // 6. Verificaciones finales del Reto Integrador[cite: 1]
    boolean caseExists = rescueCaseRepository.findByCaseCode("RES-2026-100").isPresent();
    assertTrue(caseExists);

    var treatments = treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(savedAnimal.getId());
    assertNotNull(treatments);
    assertEquals(2, treatments.size());
    assertEquals(TreatmentType.WOUND_CARE, treatments.get(0).getType());
    assertEquals(TreatmentType.HYDRATION, treatments.get(1).getType());
}


@Test
public void testRetoIntegradorValidations() {
    // 1. Centro de Rescate
    RescueCenter center = new RescueCenter();
    center.setCode("DB-CAR");
    center.setName("DeepBlue Caribbean");
    center.setCity("Santa Marta");
    RescueCenter savedCenter = rescueCenterRepository.save(center);

    // 2. Caso de Rescate
    RescueCase rescueCase = new RescueCase();
    rescueCase.setCaseCode("RES-2026-100");
    rescueCase.setRescueDate(LocalDate.of(2026, 8, 18));
    rescueCase.setRescueLocation("Bahía Concha");
    rescueCase.setStatus(RescueStatus.IN_REHABILITATION);
    rescueCase.setRescueCenter(savedCenter);
    RescueCase savedCase = rescueCaseRepository.save(rescueCase);

    // 3. Animal y Expediente Médico
    Animal animal = new Animal();
    animal.setAnimalCode("AN-2026-100");
    animal.setCommonName("Green Sea Turtle");
    animal.setScientificName("Chelonia mydas");
    animal.setSex(AnimalSex.FEMALE);
    savedCase.assignAnimal(animal);

    MedicalRecord medicalRecord = new MedicalRecord();
    medicalRecord.setInitialWeight(new java.math.BigDecimal("27.80"));
    medicalRecord.setInitialCondition("STABLE");
    medicalRecord.setInjuries("Injury caused by fishing net");
    medicalRecord.setObservations("Possible plastic ingestion");
    animal.assignMedicalRecord(medicalRecord);
    Animal savedAnimal = animalRepository.save(animal);

    // 4. Especialista y Experiencias
    Expertise marineReptiles = expertiseRepository.findByNameIgnoreCase("Marine Reptiles").orElseThrow();
    Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
    Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();

    Specialist specialist = new Specialist();
    specialist.setProfessionalCode("SPEC-001");
    specialist.setFirstName("Elena");
    specialist.setLastName("Vargas");
    specialist.setEmail("elena@deepblue.org");
    specialist.setActive(true);
    specialist.addExpertise(marineReptiles);
    specialist.addExpertise(trauma);
    specialist.addExpertise(rehabilitation);
    Specialist savedSpecialist = specialistRepository.save(specialist);

    // 5. Tratamientos
    Treatment t1 = new Treatment();
    t1.setAnimal(savedAnimal);
    t1.setSpecialist(savedSpecialist);
    t1.setPerformedAt(LocalDateTime.of(2026, 8, 18, 10, 0));
    t1.setType(TreatmentType.WOUND_CARE);
    t1.setDescription("Cleaning of left front flipper");
    treatmentRepository.save(t1);

    Treatment t2 = new Treatment();
    t2.setAnimal(savedAnimal);
    t2.setSpecialist(savedSpecialist);
    t2.setPerformedAt(LocalDateTime.of(2026, 8, 18, 11, 0));
    t2.setType(TreatmentType.HYDRATION);
    t2.setDescription("Subcutaneous fluid therapy");
    treatmentRepository.save(t2);

    // --- ASERCIONES Y VALIDACIONES ---
    boolean caseExists = rescueCaseRepository.findByCaseCode("RES-2026-100").isPresent();
    assertTrue(caseExists, "El caso de rescate RES-2026-100 debería existir en la base de datos");

    Animal foundAnimal = animalRepository.findByAnimalCode("AN-2026-100").orElseThrow();
    assertNotNull(foundAnimal.getMedicalRecord(), "El animal debe tener un expediente médico asociado");
    assertEquals("STABLE", foundAnimal.getMedicalRecord().getInitialCondition());

    List<Treatment> treatments = treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(foundAnimal.getId());
    assertNotNull(treatments);
    assertEquals(2, treatments.size(), "Deberían registrarse exactamente 2 tratamientos");
    assertEquals(TreatmentType.WOUND_CARE, treatments.get(0).getType());
    assertEquals(TreatmentType.HYDRATION, treatments.get(1).getType());

    Specialist foundSpecialist = specialistRepository.findByProfessionalCode("SPEC-001").orElseThrow();
    assertEquals(3, foundSpecialist.getExpertiseAreas().size(), "El especialista debe tener 3 áreas de experiencia");

}

}
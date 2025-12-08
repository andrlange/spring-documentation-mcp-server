package com.spring.mcp.integration;

import com.spring.mcp.config.TestContainersConfig;
import com.spring.mcp.model.entity.*;
import com.spring.mcp.model.enums.JavadocClassKind;
import com.spring.mcp.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Javadoc JPA repositories.
 * Tests database operations with a real PostgreSQL container.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
@DisplayName("Javadoc Repository Integration Tests")
@Transactional
class JavadocRepositoryIntegrationTest {

    @Autowired
    private JavadocPackageRepository packageRepository;

    @Autowired
    private JavadocClassRepository classRepository;

    @Autowired
    private JavadocMethodRepository methodRepository;

    @Autowired
    private JavadocFieldRepository fieldRepository;

    @Autowired
    private JavadocConstructorRepository constructorRepository;

    // Use unique IDs for each test to avoid conflicts
    private static final AtomicLong testIdCounter = new AtomicLong(System.currentTimeMillis());
    private String testId;
    private String library;
    private String version;
    private String packageName;

    @BeforeEach
    void setUp() {
        testId = String.valueOf(testIdCounter.incrementAndGet());
        library = "test-lib-" + testId;
        version = "1.0.0";
        packageName = "com.test.pkg" + testId;
    }

    @Nested
    @DisplayName("JavadocPackageRepository Tests")
    class PackageRepositoryTests {

        @Test
        @DisplayName("Should save and retrieve package")
        void shouldSaveAndRetrievePackage() {
            // Given
            JavadocPackage pkg = createTestPackage();

            // When
            JavadocPackage saved = packageRepository.save(pkg);
            packageRepository.flush();
            Optional<JavadocPackage> found = packageRepository.findByLibraryNameAndVersionAndPackageName(
                    library, version, packageName);

            // Then
            assertThat(saved.getId()).isNotNull();
            assertThat(found).isPresent();
            assertThat(found.get().getPackageName()).isEqualTo(packageName);
            assertThat(found.get().getSummary()).isEqualTo("Test package summary");
        }

        @Test
        @DisplayName("Should find packages by library and version")
        void shouldFindPackagesByLibraryAndVersion() {
            // Given
            JavadocPackage pkg1 = createTestPackage();
            pkg1.setPackageName(packageName + ".one");

            JavadocPackage pkg2 = createTestPackage();
            pkg2.setPackageName(packageName + ".two");

            packageRepository.saveAll(List.of(pkg1, pkg2));
            packageRepository.flush();

            // When
            List<JavadocPackage> found = packageRepository.findByLibraryNameAndVersion(library, version);

            // Then
            assertThat(found).hasSize(2);
        }

        @Test
        @DisplayName("Should count packages by library and version")
        void shouldCountPackages() {
            // Given
            JavadocPackage pkg1 = createTestPackage();
            pkg1.setPackageName(packageName + ".one");

            JavadocPackage pkg2 = createTestPackage();
            pkg2.setPackageName(packageName + ".two");

            packageRepository.saveAll(List.of(pkg1, pkg2));
            packageRepository.flush();

            // When
            long count = packageRepository.countByLibraryNameAndVersion(library, version);

            // Then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("Should find distinct library names")
        void shouldFindDistinctLibraryNames() {
            // Given
            JavadocPackage pkg1 = createTestPackage();
            pkg1.setLibraryName(library + "-a");

            JavadocPackage pkg2 = createTestPackage();
            pkg2.setLibraryName(library + "-b");
            pkg2.setPackageName(packageName + ".other");

            packageRepository.saveAll(List.of(pkg1, pkg2));
            packageRepository.flush();

            // When
            List<String> libraries = packageRepository.findDistinctLibraryNames();

            // Then
            assertThat(libraries).contains(library + "-a", library + "-b");
        }

        @Test
        @DisplayName("Should search packages by name pattern")
        void shouldSearchPackagesByNamePattern() {
            // Given
            JavadocPackage pkg = createTestPackage();
            packageRepository.save(pkg);
            packageRepository.flush();

            // When - use searchByPackageName instead of full-text search
            List<JavadocPackage> found = packageRepository.searchByPackageName(library, version, "pkg");

            // Then
            assertThat(found).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("JavadocClassRepository Tests")
    class ClassRepositoryTests {

        @Test
        @DisplayName("Should save class with members and retrieve")
        void shouldSaveClassWithMembers() {
            // Given
            JavadocPackage pkg = packageRepository.save(createTestPackage());
            packageRepository.flush();
            JavadocClass cls = createTestClass(pkg);

            // When
            JavadocClass saved = classRepository.save(cls);
            classRepository.flush();
            Optional<JavadocClass> found = classRepository.findByLibraryVersionAndFqcn(
                    library, version, packageName + ".TestClass");

            // Then
            assertThat(saved.getId()).isNotNull();
            assertThat(found).isPresent();
            assertThat(found.get().getSimpleName()).isEqualTo("TestClass");
            assertThat(found.get().getMethods()).hasSize(1);
            assertThat(found.get().getFields()).hasSize(1);
            assertThat(found.get().getConstructors()).hasSize(1);
        }

        @Test
        @DisplayName("Should find classes by package ID")
        void shouldFindClassesByPackageId() {
            // Given
            JavadocPackage pkg = packageRepository.save(createTestPackage());
            packageRepository.flush();

            JavadocClass cls1 = createTestClass(pkg);
            cls1.setSimpleName("ClassOne");
            cls1.setFqcn(packageName + ".ClassOne");

            JavadocClass cls2 = createTestClass(pkg);
            cls2.setSimpleName("ClassTwo");
            cls2.setFqcn(packageName + ".ClassTwo");

            classRepository.saveAll(List.of(cls1, cls2));
            classRepository.flush();

            // When
            List<JavadocClass> found = classRepository.findByPackageId(pkg.getId());

            // Then
            assertThat(found).hasSize(2);
        }

        @Test
        @DisplayName("Should count classes by library and version")
        void shouldCountClasses() {
            // Given
            JavadocPackage pkg = packageRepository.save(createTestPackage());
            packageRepository.flush();

            JavadocClass cls1 = createTestClass(pkg);
            cls1.setFqcn(packageName + ".ClassOne");
            JavadocClass cls2 = createTestClass(pkg);
            cls2.setFqcn(packageName + ".ClassTwo");

            classRepository.saveAll(List.of(cls1, cls2));
            classRepository.flush();

            // When
            long count = classRepository.countByLibraryAndVersion(library, version);

            // Then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("Should handle deprecated classes")
        void shouldHandleDeprecatedClasses() {
            // Given
            JavadocPackage pkg = packageRepository.save(createTestPackage());
            packageRepository.flush();
            JavadocClass cls = createTestClass(pkg);
            cls.setDeprecated(true);
            cls.setDeprecatedMessage("Use NewClass instead");

            classRepository.save(cls);
            classRepository.flush();

            // When
            Optional<JavadocClass> found = classRepository.findByLibraryVersionAndFqcn(
                    library, version, cls.getFqcn());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getDeprecated()).isTrue();
            assertThat(found.get().getDeprecatedMessage()).contains("NewClass");
        }
    }

    @Nested
    @DisplayName("Member Count Tests")
    class MemberCountTests {

        @Test
        @DisplayName("Should count methods by library and version")
        void shouldCountMethods() {
            // Given
            JavadocPackage pkg = packageRepository.save(createTestPackage());
            packageRepository.flush();
            classRepository.save(createTestClass(pkg));
            classRepository.flush();

            // When
            long count = methodRepository.countByLibraryAndVersion(library, version);

            // Then
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("Should count fields by library and version")
        void shouldCountFields() {
            // Given
            JavadocPackage pkg = packageRepository.save(createTestPackage());
            packageRepository.flush();
            classRepository.save(createTestClass(pkg));
            classRepository.flush();

            // When
            long count = fieldRepository.countByLibraryAndVersion(library, version);

            // Then
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("Should count constructors by library and version")
        void shouldCountConstructors() {
            // Given
            JavadocPackage pkg = packageRepository.save(createTestPackage());
            packageRepository.flush();
            classRepository.save(createTestClass(pkg));
            classRepository.flush();

            // When
            long count = constructorRepository.countByLibraryAndVersion(library, version);

            // Then
            assertThat(count).isEqualTo(1);
        }
    }

    // Helper methods
    private JavadocPackage createTestPackage() {
        JavadocPackage pkg = new JavadocPackage();
        pkg.setLibraryName(library);
        pkg.setVersion(version);
        pkg.setPackageName(packageName);
        pkg.setSummary("Test package summary");
        pkg.setDescription("Full test package description");
        pkg.setSourceUrl("https://example.com/api/" + packageName.replace('.', '/'));
        return pkg;
    }

    private JavadocClass createTestClass(JavadocPackage pkg) {
        JavadocClass cls = new JavadocClass();
        cls.setJavadocPackage(pkg);
        cls.setFqcn(packageName + ".TestClass");
        cls.setSimpleName("TestClass");
        cls.setKind(JavadocClassKind.CLASS);
        cls.setModifiers("public");
        cls.setSummary("Test class summary");
        cls.setDescription("Full test class description");
        cls.setInterfaces(new ArrayList<>());
        cls.setAnnotations(new ArrayList<>());
        cls.setSourceUrl("https://example.com/api/" + packageName.replace('.', '/') + "/TestClass.html");

        // Add method
        JavadocMethod method = new JavadocMethod();
        method.setJavadocClass(cls);
        method.setName("testMethod");
        method.setSignature("public void testMethod()");
        method.setReturnType("void");
        method.setSummary("Test method");
        method.setParameters(new ArrayList<>());
        method.setThrowsList(new ArrayList<>());
        method.setAnnotations(new ArrayList<>());
        cls.setMethods(new HashSet<>(Set.of(method)));

        // Add field
        JavadocField field = new JavadocField();
        field.setJavadocClass(cls);
        field.setName("testField");
        field.setType("String");
        field.setModifiers("private");
        field.setSummary("Test field");
        cls.setFields(new HashSet<>(Set.of(field)));

        // Add constructor
        JavadocConstructor ctor = new JavadocConstructor();
        ctor.setJavadocClass(cls);
        ctor.setSignature("public TestClass()");
        ctor.setSummary("Default constructor");
        ctor.setParameters(new ArrayList<>());
        ctor.setThrowsList(new ArrayList<>());
        ctor.setAnnotations(new ArrayList<>());
        cls.setConstructors(new HashSet<>(Set.of(ctor)));

        return cls;
    }
}

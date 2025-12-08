package com.spring.mcp.service.javadoc;

import com.spring.mcp.model.entity.*;
import com.spring.mcp.model.enums.JavadocClassKind;
import com.spring.mcp.repository.*;
import com.spring.mcp.service.javadoc.dto.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JavadocStorageService.
 * Uses Mockito to mock repository dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Javadoc Storage Service Tests")
class JavadocStorageServiceTest {

    @Mock
    private JavadocPackageRepository packageRepository;

    @Mock
    private JavadocClassRepository classRepository;

    @Mock
    private JavadocMethodRepository methodRepository;

    @Mock
    private JavadocFieldRepository fieldRepository;

    @Mock
    private JavadocConstructorRepository constructorRepository;

    @InjectMocks
    private JavadocStorageService storageService;

    private static final String LIBRARY_NAME = "spring-framework";
    private static final String VERSION = "6.2.1";
    private static final String PACKAGE_NAME = "org.springframework.web.client";

    @Nested
    @DisplayName("Package Storage Tests")
    class PackageStorageTests {

        @Test
        @DisplayName("Should save new package")
        void shouldSaveNewPackage() {
            // Given
            ParsedPackageDto dto = ParsedPackageDto.builder()
                    .packageName(PACKAGE_NAME)
                    .summary("Client-side web support")
                    .description("Full description of the package")
                    .sourceUrl("https://docs.spring.io/test/package-summary.html")
                    .classes(List.of("RestTemplate", "RestClient"))
                    .build();

            when(packageRepository.findByLibraryNameAndVersionAndPackageName(
                    LIBRARY_NAME, VERSION, PACKAGE_NAME)).thenReturn(Optional.empty());

            when(packageRepository.save(any(JavadocPackage.class))).thenAnswer(invocation -> {
                JavadocPackage pkg = invocation.getArgument(0);
                pkg.setId(1L);
                return pkg;
            });

            // When
            JavadocPackage result = storageService.savePackage(dto, LIBRARY_NAME, VERSION);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getLibraryName()).isEqualTo(LIBRARY_NAME);
            assertThat(result.getVersion()).isEqualTo(VERSION);
            assertThat(result.getPackageName()).isEqualTo(PACKAGE_NAME);
            assertThat(result.getSummary()).isEqualTo("Client-side web support");

            verify(packageRepository).save(any(JavadocPackage.class));
        }

        @Test
        @DisplayName("Should return existing package without updating (Javadocs are immutable)")
        void shouldReturnExistingPackageWithoutUpdate() {
            // Given
            ParsedPackageDto dto = ParsedPackageDto.builder()
                    .packageName(PACKAGE_NAME)
                    .summary("Updated summary")
                    .description("Updated description")
                    .sourceUrl("https://docs.spring.io/test/package-summary.html")
                    .classes(List.of())
                    .build();

            JavadocPackage existingPkg = new JavadocPackage();
            existingPkg.setId(1L);
            existingPkg.setLibraryName(LIBRARY_NAME);
            existingPkg.setVersion(VERSION);
            existingPkg.setPackageName(PACKAGE_NAME);
            existingPkg.setSummary("Old summary");

            when(packageRepository.findByLibraryNameAndVersionAndPackageName(
                    LIBRARY_NAME, VERSION, PACKAGE_NAME)).thenReturn(Optional.of(existingPkg));

            // When
            JavadocPackage result = storageService.savePackage(dto, LIBRARY_NAME, VERSION);

            // Then - should return existing package without updating
            assertThat(result).isNotNull();
            assertThat(result).isSameAs(existingPkg);
            assertThat(result.getSummary()).isEqualTo("Old summary"); // NOT updated
            verify(packageRepository, never()).save(any()); // save should NOT be called
        }
    }

    @Nested
    @DisplayName("Class Storage Tests")
    class ClassStorageTests {

        private JavadocPackage testPackage;

        @BeforeEach
        void setUp() {
            testPackage = new JavadocPackage();
            testPackage.setId(1L);
            testPackage.setLibraryName(LIBRARY_NAME);
            testPackage.setVersion(VERSION);
            testPackage.setPackageName(PACKAGE_NAME);
        }

        @Test
        @DisplayName("Should save new class with methods, fields, and constructors")
        void shouldSaveNewClassWithMembers() {
            // Given
            ParsedClassDto dto = ParsedClassDto.builder()
                    .simpleName("RestTemplate")
                    .kind(JavadocClassKind.CLASS)
                    .modifiers("public")
                    .summary("Synchronous HTTP client")
                    .description("Full description")
                    .superClass("Object")
                    .interfaces(List.of("RestOperations"))
                    .deprecated(true)
                    .deprecatedMessage("Use RestClient instead")
                    .sourceUrl("https://docs.spring.io/test/RestTemplate.html")
                    .annotations(List.of("@Deprecated"))
                    .methods(List.of(
                            ParsedMethodDto.builder()
                                    .name("getForObject")
                                    .signature("public <T> T getForObject(String url, Class<T> responseType)")
                                    .returnType("T")
                                    .summary("GET request")
                                    .parameters(List.of(
                                            new MethodParameterDto("url", "String", "The URL"),
                                            new MethodParameterDto("responseType", "Class<T>", "Response type")
                                    ))
                                    .build()
                    ))
                    .fields(List.of(
                            ParsedFieldDto.builder()
                                    .name("logger")
                                    .type("Log")
                                    .modifiers("protected static final")
                                    .summary("Logger for subclasses")
                                    .build()
                    ))
                    .constructors(List.of(
                            ParsedConstructorDto.builder()
                                    .signature("public RestTemplate()")
                                    .summary("Default constructor")
                                    .parameters(List.of())
                                    .build()
                    ))
                    .build();

            String expectedFqcn = PACKAGE_NAME + ".RestTemplate";

            // Class does not exist yet
            when(classRepository.existsByLibraryVersionAndFqcn(LIBRARY_NAME, VERSION, expectedFqcn))
                    .thenReturn(false);

            when(classRepository.save(any(JavadocClass.class))).thenAnswer(invocation -> {
                JavadocClass cls = invocation.getArgument(0);
                cls.setId(1L);
                return cls;
            });

            // When
            JavadocClass result = storageService.saveClass(dto, testPackage);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getFqcn()).isEqualTo(expectedFqcn);
            assertThat(result.getSimpleName()).isEqualTo("RestTemplate");
            assertThat(result.getKind()).isEqualTo(JavadocClassKind.CLASS);
            assertThat(result.getModifiers()).isEqualTo("public");
            assertThat(result.getDeprecated()).isTrue();
            assertThat(result.getDeprecatedMessage()).contains("RestClient");
            assertThat(result.getMethods()).hasSize(1);
            assertThat(result.getFields()).hasSize(1);
            assertThat(result.getConstructors()).hasSize(1);

            verify(classRepository, times(2)).save(any(JavadocClass.class));
        }

        @Test
        @DisplayName("Should return null for existing class (Javadocs are immutable)")
        void shouldReturnNullForExistingClass() {
            // Given
            ParsedClassDto dto = ParsedClassDto.builder()
                    .simpleName("RestTemplate")
                    .kind(JavadocClassKind.CLASS)
                    .modifiers("public")
                    .summary("Updated summary")
                    .interfaces(List.of())
                    .annotations(List.of())
                    .methods(List.of())
                    .fields(List.of())
                    .constructors(List.of())
                    .build();

            String expectedFqcn = PACKAGE_NAME + ".RestTemplate";

            // Class already exists
            when(classRepository.existsByLibraryVersionAndFqcn(LIBRARY_NAME, VERSION, expectedFqcn))
                    .thenReturn(true);

            // When
            JavadocClass result = storageService.saveClass(dto, testPackage);

            // Then - should return null and NOT save anything
            assertThat(result).isNull();
            verify(classRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should build FQCN from package name when not provided")
        void shouldBuildFqcnFromPackage() {
            // Given
            ParsedClassDto dto = ParsedClassDto.builder()
                    .simpleName("MyClass")
                    .kind(JavadocClassKind.CLASS)
                    .interfaces(List.of())
                    .annotations(List.of())
                    .methods(List.of())
                    .fields(List.of())
                    .constructors(List.of())
                    .build();

            String expectedFqcn = PACKAGE_NAME + ".MyClass";

            // Class does not exist yet
            when(classRepository.existsByLibraryVersionAndFqcn(LIBRARY_NAME, VERSION, expectedFqcn))
                    .thenReturn(false);
            when(classRepository.save(any(JavadocClass.class))).thenAnswer(invocation -> {
                JavadocClass cls = invocation.getArgument(0);
                cls.setId(1L);
                return cls;
            });

            // When
            JavadocClass result = storageService.saveClass(dto, testPackage);

            // Then
            assertThat(result.getFqcn()).isEqualTo(expectedFqcn);
        }
    }

    @Nested
    @DisplayName("Library Management Tests")
    class LibraryManagementTests {

        @Test
        @DisplayName("Should clear library version data")
        void shouldClearLibraryVersion() {
            // Given
            List<JavadocPackage> packages = List.of(
                    new JavadocPackage(),
                    new JavadocPackage(),
                    new JavadocPackage()
            );
            when(packageRepository.findByLibraryNameAndVersion(LIBRARY_NAME, VERSION))
                    .thenReturn(packages);

            // When
            int result = storageService.clearLibraryVersion(LIBRARY_NAME, VERSION);

            // Then
            assertThat(result).isEqualTo(3);
            verify(packageRepository).deleteByLibraryNameAndVersion(LIBRARY_NAME, VERSION);
        }

        @Test
        @DisplayName("Should return zero when no packages to clear")
        void shouldReturnZeroWhenNoPackages() {
            // Given
            when(packageRepository.findByLibraryNameAndVersion(LIBRARY_NAME, VERSION))
                    .thenReturn(List.of());

            // When
            int result = storageService.clearLibraryVersion(LIBRARY_NAME, VERSION);

            // Then
            assertThat(result).isZero();
            verify(packageRepository, never()).deleteByLibraryNameAndVersion(any(), any());
        }

        @Test
        @DisplayName("Should check if version exists")
        void shouldCheckIfVersionExists() {
            // Given
            when(packageRepository.existsByLibraryNameAndVersion(LIBRARY_NAME, VERSION))
                    .thenReturn(true);

            // When
            boolean result = storageService.existsForVersion(LIBRARY_NAME, VERSION);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should get version statistics")
        void shouldGetVersionStatistics() {
            // Given
            when(packageRepository.countByLibraryNameAndVersion(LIBRARY_NAME, VERSION)).thenReturn(10L);
            when(classRepository.countByLibraryAndVersion(LIBRARY_NAME, VERSION)).thenReturn(50L);
            when(methodRepository.countByLibraryAndVersion(LIBRARY_NAME, VERSION)).thenReturn(500L);
            when(fieldRepository.countByLibraryAndVersion(LIBRARY_NAME, VERSION)).thenReturn(100L);
            when(constructorRepository.countByLibraryAndVersion(LIBRARY_NAME, VERSION)).thenReturn(75L);

            // When
            Map<String, Long> stats = storageService.getVersionStats(LIBRARY_NAME, VERSION);

            // Then
            assertThat(stats)
                    .containsEntry("packages", 10L)
                    .containsEntry("classes", 50L)
                    .containsEntry("methods", 500L)
                    .containsEntry("fields", 100L)
                    .containsEntry("constructors", 75L);
        }

        @Test
        @DisplayName("Should get all library names")
        void shouldGetLibraryNames() {
            // Given
            List<String> libraries = List.of("spring-framework", "spring-boot", "spring-ai");
            when(packageRepository.findDistinctLibraryNames()).thenReturn(libraries);

            // When
            List<String> result = storageService.getLibraryNames();

            // Then
            assertThat(result).containsExactly("spring-framework", "spring-boot", "spring-ai");
        }

        @Test
        @DisplayName("Should get versions for library")
        void shouldGetVersionsForLibrary() {
            // Given
            List<String> versions = List.of("6.2.1", "6.1.0", "6.0.0");
            when(packageRepository.findVersionsByLibraryName(LIBRARY_NAME)).thenReturn(versions);

            // When
            List<String> result = storageService.getVersions(LIBRARY_NAME);

            // Then
            assertThat(result).containsExactly("6.2.1", "6.1.0", "6.0.0");
        }
    }

    @Nested
    @DisplayName("Method Parameter Conversion Tests")
    class MethodParameterTests {

        private JavadocPackage testPackage;

        @BeforeEach
        void setUp() {
            testPackage = new JavadocPackage();
            testPackage.setId(1L);
            testPackage.setLibraryName(LIBRARY_NAME);
            testPackage.setVersion(VERSION);
            testPackage.setPackageName(PACKAGE_NAME);
        }

        @Test
        @DisplayName("Should convert method parameters to Map format")
        void shouldConvertMethodParameters() {
            // Given
            ParsedClassDto dto = ParsedClassDto.builder()
                    .simpleName("TestClass")
                    .kind(JavadocClassKind.CLASS)
                    .interfaces(List.of())
                    .annotations(List.of())
                    .methods(List.of(
                            ParsedMethodDto.builder()
                                    .name("testMethod")
                                    .parameters(List.of(
                                            new MethodParameterDto("param1", "String", "First parameter"),
                                            new MethodParameterDto("param2", "int", "Second parameter")
                                    ))
                                    .build()
                    ))
                    .fields(List.of())
                    .constructors(List.of())
                    .build();

            // Class does not exist yet
            when(classRepository.existsByLibraryVersionAndFqcn(anyString(), anyString(), anyString()))
                    .thenReturn(false);
            when(classRepository.save(any(JavadocClass.class))).thenAnswer(invocation -> {
                JavadocClass cls = invocation.getArgument(0);
                cls.setId(1L);
                return cls;
            });

            // When
            JavadocClass result = storageService.saveClass(dto, testPackage);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getMethods()).hasSize(1);
            JavadocMethod method = result.getMethods().iterator().next();
            assertThat(method.getParameters()).hasSize(2);
            assertThat(method.getParameters().get(0))
                    .containsEntry("name", "param1")
                    .containsEntry("type", "String")
                    .containsEntry("description", "First parameter");
        }
    }
}

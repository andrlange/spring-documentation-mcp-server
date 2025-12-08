package com.spring.mcp.service.tools;

import com.spring.mcp.config.JavadocsFeatureConfig;
import com.spring.mcp.model.entity.*;
import com.spring.mcp.model.enums.JavadocClassKind;
import com.spring.mcp.repository.*;
import com.spring.mcp.service.javadoc.JavadocVersionService;
import com.spring.mcp.service.tools.dto.javadoc.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JavadocTools MCP service.
 * Tests the MCP tool methods for Javadoc documentation queries.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Javadoc Tools Tests")
class JavadocToolsTest {

    @Mock
    private JavadocClassRepository classRepository;

    @Mock
    private JavadocPackageRepository packageRepository;

    @Mock
    private JavadocVersionService versionService;

    @Mock
    private JavadocsFeatureConfig config;

    @InjectMocks
    private JavadocTools javadocTools;

    private static final String LIBRARY = "spring-ai";
    private static final String VERSION = "1.1.1";
    private static final String PACKAGE_NAME = "org.springframework.ai.chat";
    private static final String CLASS_FQCN = "org.springframework.ai.chat.ChatClient";

    @BeforeEach
    void setUp() {
        when(config.isEnabled()).thenReturn(true);
    }

    @Nested
    @DisplayName("getClassDoc Tests")
    class GetClassDocTests {

        @Test
        @DisplayName("Should return class documentation")
        void shouldReturnClassDoc() {
            // Given
            JavadocPackage pkg = createTestPackage();
            JavadocClass cls = createTestClass(pkg);

            when(versionService.resolveVersion(LIBRARY, VERSION)).thenReturn(Optional.of(VERSION));
            when(classRepository.findByLibraryVersionAndFqcn(LIBRARY, VERSION, CLASS_FQCN))
                    .thenReturn(Optional.of(cls));

            // When
            ClassDocResult result = javadocTools.getClassDoc(LIBRARY, VERSION, CLASS_FQCN);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.fqcn()).isEqualTo(CLASS_FQCN);
            assertThat(result.simpleName()).isEqualTo("ChatClient");
            assertThat(result.kind()).isEqualTo("INTERFACE");
            assertThat(result.summary()).isEqualTo("Client to perform AI chat requests");
            assertThat(result.methods()).hasSize(1);
            assertThat(result.fields()).hasSize(1);
            assertThat(result.constructors()).hasSize(1);
        }

        @Test
        @DisplayName("Should return notFound when feature is disabled")
        void shouldReturnNotFoundWhenDisabled() {
            // Given
            when(config.isEnabled()).thenReturn(false);

            // When
            ClassDocResult result = javadocTools.getClassDoc(LIBRARY, VERSION, CLASS_FQCN);

            // Then
            assertThat(result.fqcn()).isEqualTo(CLASS_FQCN);
            assertThat(result.methods()).isEmpty();
        }

        @Test
        @DisplayName("Should return notFound when version not found")
        void shouldReturnNotFoundWhenVersionNotFound() {
            // Given
            when(versionService.resolveVersion(LIBRARY, VERSION)).thenReturn(Optional.empty());

            // When
            ClassDocResult result = javadocTools.getClassDoc(LIBRARY, VERSION, CLASS_FQCN);

            // Then
            assertThat(result.fqcn()).isEqualTo(CLASS_FQCN);
            assertThat(result.methods()).isEmpty();
        }

        @Test
        @DisplayName("Should return notFound when class not found")
        void shouldReturnNotFoundWhenClassNotFound() {
            // Given
            when(versionService.resolveVersion(LIBRARY, VERSION)).thenReturn(Optional.of(VERSION));
            when(classRepository.findByLibraryVersionAndFqcn(LIBRARY, VERSION, CLASS_FQCN))
                    .thenReturn(Optional.empty());

            // When
            ClassDocResult result = javadocTools.getClassDoc(LIBRARY, VERSION, CLASS_FQCN);

            // Then
            assertThat(result.fqcn()).isEqualTo(CLASS_FQCN);
            assertThat(result.methods()).isEmpty();
        }

        @Test
        @DisplayName("Should handle deprecated class")
        void shouldHandleDeprecatedClass() {
            // Given
            JavadocPackage pkg = createTestPackage();
            JavadocClass cls = createTestClass(pkg);
            cls.setDeprecated(true);
            cls.setDeprecatedMessage("Use new API");

            when(versionService.resolveVersion(LIBRARY, VERSION)).thenReturn(Optional.of(VERSION));
            when(classRepository.findByLibraryVersionAndFqcn(LIBRARY, VERSION, CLASS_FQCN))
                    .thenReturn(Optional.of(cls));

            // When
            ClassDocResult result = javadocTools.getClassDoc(LIBRARY, VERSION, CLASS_FQCN);

            // Then
            assertThat(result.deprecated()).isTrue();
            assertThat(result.deprecatedMessage()).isEqualTo("Use new API");
        }

        @Test
        @DisplayName("Should sort methods by name")
        void shouldSortMethodsByName() {
            // Given
            JavadocPackage pkg = createTestPackage();
            JavadocClass cls = createTestClass(pkg);
            cls.getMethods().clear();

            // Add methods out of order (must set IDs for HashSet equality)
            JavadocMethod methodZ = new JavadocMethod();
            methodZ.setId(100L);
            methodZ.setName("zMethod");
            methodZ.setJavadocClass(cls);
            cls.getMethods().add(methodZ);

            JavadocMethod methodA = new JavadocMethod();
            methodA.setId(101L);
            methodA.setName("aMethod");
            methodA.setJavadocClass(cls);
            cls.getMethods().add(methodA);

            when(versionService.resolveVersion(LIBRARY, VERSION)).thenReturn(Optional.of(VERSION));
            when(classRepository.findByLibraryVersionAndFqcn(LIBRARY, VERSION, CLASS_FQCN))
                    .thenReturn(Optional.of(cls));

            // When
            ClassDocResult result = javadocTools.getClassDoc(LIBRARY, VERSION, CLASS_FQCN);

            // Then
            assertThat(result.methods()).hasSize(2);
            assertThat(result.methods().get(0).name()).isEqualTo("aMethod");
            assertThat(result.methods().get(1).name()).isEqualTo("zMethod");
        }
    }

    @Nested
    @DisplayName("getPackageDoc Tests")
    class GetPackageDocTests {

        @Test
        @DisplayName("Should return package documentation with classes")
        void shouldReturnPackageDoc() {
            // Given
            JavadocPackage pkg = createTestPackage();
            List<JavadocClass> classes = List.of(createTestClass(pkg));

            when(versionService.resolveVersion(LIBRARY, VERSION)).thenReturn(Optional.of(VERSION));
            when(packageRepository.findByLibraryNameAndVersionAndPackageName(LIBRARY, VERSION, PACKAGE_NAME))
                    .thenReturn(Optional.of(pkg));
            when(classRepository.findByPackageId(pkg.getId())).thenReturn(classes);

            // When
            PackageDocResult result = javadocTools.getPackageDoc(LIBRARY, VERSION, PACKAGE_NAME);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.packageName()).isEqualTo(PACKAGE_NAME);
            assertThat(result.summary()).isEqualTo("Chat API package");
            assertThat(result.classes()).hasSize(1);
            assertThat(result.totalClasses()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return notFound when feature is disabled")
        void shouldReturnNotFoundWhenDisabled() {
            // Given
            when(config.isEnabled()).thenReturn(false);

            // When
            PackageDocResult result = javadocTools.getPackageDoc(LIBRARY, VERSION, PACKAGE_NAME);

            // Then
            assertThat(result.packageName()).isEqualTo(PACKAGE_NAME);
            assertThat(result.classes()).isEmpty();
        }

        @Test
        @DisplayName("Should return notFound when package not found")
        void shouldReturnNotFoundWhenPackageNotFound() {
            // Given
            when(versionService.resolveVersion(LIBRARY, VERSION)).thenReturn(Optional.of(VERSION));
            when(packageRepository.findByLibraryNameAndVersionAndPackageName(LIBRARY, VERSION, PACKAGE_NAME))
                    .thenReturn(Optional.empty());

            // When
            PackageDocResult result = javadocTools.getPackageDoc(LIBRARY, VERSION, PACKAGE_NAME);

            // Then
            assertThat(result.packageName()).isEqualTo(PACKAGE_NAME);
            assertThat(result.classes()).isEmpty();
        }

        @Test
        @DisplayName("Should sort classes by name")
        void shouldSortClassesByName() {
            // Given
            JavadocPackage pkg = createTestPackage();

            JavadocClass classZ = new JavadocClass();
            classZ.setSimpleName("ZClass");
            classZ.setFqcn(PACKAGE_NAME + ".ZClass");
            classZ.setJavadocPackage(pkg);
            classZ.setKind(JavadocClassKind.CLASS);

            JavadocClass classA = new JavadocClass();
            classA.setSimpleName("AClass");
            classA.setFqcn(PACKAGE_NAME + ".AClass");
            classA.setJavadocPackage(pkg);
            classA.setKind(JavadocClassKind.CLASS);

            List<JavadocClass> classes = List.of(classZ, classA);

            when(versionService.resolveVersion(LIBRARY, VERSION)).thenReturn(Optional.of(VERSION));
            when(packageRepository.findByLibraryNameAndVersionAndPackageName(LIBRARY, VERSION, PACKAGE_NAME))
                    .thenReturn(Optional.of(pkg));
            when(classRepository.findByPackageId(pkg.getId())).thenReturn(classes);

            // When
            PackageDocResult result = javadocTools.getPackageDoc(LIBRARY, VERSION, PACKAGE_NAME);

            // Then
            assertThat(result.classes()).hasSize(2);
            assertThat(result.classes().get(0).simpleName()).isEqualTo("AClass");
            assertThat(result.classes().get(1).simpleName()).isEqualTo("ZClass");
        }
    }

    @Nested
    @DisplayName("searchJavadocs Tests")
    class SearchJavadocsTests {

        @Test
        @DisplayName("Should search within library/version")
        void shouldSearchWithinLibraryVersion() {
            // Given
            JavadocPackage pkg = createTestPackage();
            JavadocClass cls = createTestClass(pkg);

            when(versionService.resolveVersion(LIBRARY, VERSION)).thenReturn(Optional.of(VERSION));
            when(classRepository.searchByKeyword(LIBRARY, VERSION, "chat", 10))
                    .thenReturn(List.of(cls));

            // When
            List<JavadocSearchResult> results = javadocTools.searchJavadocs(LIBRARY, VERSION, "chat", 10);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).fqcn()).isEqualTo(CLASS_FQCN);
            assertThat(results.get(0).simpleName()).isEqualTo("ChatClient");
        }

        @Test
        @DisplayName("Should search globally when no library specified")
        void shouldSearchGloballyWhenNoLibrary() {
            // Given
            JavadocPackage pkg = createTestPackage();
            JavadocClass cls = createTestClass(pkg);

            when(classRepository.searchByKeywordGlobal("chat", 10))
                    .thenReturn(List.of(cls));

            // When
            List<JavadocSearchResult> results = javadocTools.searchJavadocs(null, null, "chat", 10);

            // Then
            assertThat(results).hasSize(1);
            verify(classRepository).searchByKeywordGlobal("chat", 10);
        }

        @Test
        @DisplayName("Should return empty when feature is disabled")
        void shouldReturnEmptyWhenDisabled() {
            // Given
            when(config.isEnabled()).thenReturn(false);

            // When
            List<JavadocSearchResult> results = javadocTools.searchJavadocs(LIBRARY, VERSION, "chat", 10);

            // Then
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Should return empty for blank query")
        void shouldReturnEmptyForBlankQuery() {
            // When
            List<JavadocSearchResult> results = javadocTools.searchJavadocs(LIBRARY, VERSION, "", 10);

            // Then
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Should return empty for null query")
        void shouldReturnEmptyForNullQuery() {
            // When
            List<JavadocSearchResult> results = javadocTools.searchJavadocs(LIBRARY, VERSION, null, 10);

            // Then
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Should limit results to max 50")
        void shouldLimitResultsToMax50() {
            // Given
            JavadocPackage pkg = createTestPackage();
            JavadocClass cls = createTestClass(pkg);

            when(classRepository.searchByKeywordGlobal("chat", 50))
                    .thenReturn(List.of(cls));

            // When - request 100 results
            List<JavadocSearchResult> results = javadocTools.searchJavadocs(null, null, "chat", 100);

            // Then - should cap at 50
            verify(classRepository).searchByKeywordGlobal("chat", 50);
        }

        @Test
        @DisplayName("Should default to 10 results when limit is null")
        void shouldDefaultTo10Results() {
            // Given
            JavadocPackage pkg = createTestPackage();
            JavadocClass cls = createTestClass(pkg);

            when(classRepository.searchByKeywordGlobal("chat", 10))
                    .thenReturn(List.of(cls));

            // When
            List<JavadocSearchResult> results = javadocTools.searchJavadocs(null, null, "chat", null);

            // Then
            verify(classRepository).searchByKeywordGlobal("chat", 10);
        }

        @Test
        @DisplayName("Should also search packages when room available")
        void shouldAlsoSearchPackages() {
            // Given
            JavadocPackage pkg = createTestPackage();
            JavadocClass cls = createTestClass(pkg);

            when(versionService.resolveVersion(LIBRARY, VERSION)).thenReturn(Optional.of(VERSION));
            when(classRepository.searchByKeyword(LIBRARY, VERSION, "chat", 10))
                    .thenReturn(List.of(cls)); // Returns 1 result
            when(packageRepository.searchByKeyword(LIBRARY, VERSION, "chat", 9))
                    .thenReturn(List.of(pkg)); // Search remaining 9 slots

            // When
            List<JavadocSearchResult> results = javadocTools.searchJavadocs(LIBRARY, VERSION, "chat", 10);

            // Then
            assertThat(results).hasSize(2);
            verify(packageRepository).searchByKeyword(LIBRARY, VERSION, "chat", 9);
        }
    }

    @Nested
    @DisplayName("listJavadocLibraries Tests")
    class ListJavadocLibrariesTests {

        @Test
        @DisplayName("Should list all libraries with versions")
        void shouldListAllLibraries() {
            // Given
            when(packageRepository.findDistinctLibraryNames())
                    .thenReturn(List.of("spring-ai", "spring-boot"));
            when(versionService.getAvailableVersions("spring-ai"))
                    .thenReturn(List.of("1.1.1", "1.0.0"));
            when(versionService.getLatestVersion("spring-ai"))
                    .thenReturn(Optional.of("1.1.1"));
            when(versionService.getAvailableVersions("spring-boot"))
                    .thenReturn(List.of("3.5.0"));
            when(versionService.getLatestVersion("spring-boot"))
                    .thenReturn(Optional.of("3.5.0"));

            // When
            List<JavadocTools.LibraryInfo> results = javadocTools.listJavadocLibraries();

            // Then
            assertThat(results).hasSize(2);
            assertThat(results.get(0).library()).isEqualTo("spring-ai");
            assertThat(results.get(0).versions()).containsExactly("1.1.1", "1.0.0");
            assertThat(results.get(0).latestVersion()).isEqualTo("1.1.1");
            assertThat(results.get(0).versionCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return empty when feature is disabled")
        void shouldReturnEmptyWhenDisabled() {
            // Given
            when(config.isEnabled()).thenReturn(false);

            // When
            List<JavadocTools.LibraryInfo> results = javadocTools.listJavadocLibraries();

            // Then
            assertThat(results).isEmpty();
        }
    }

    // Helper methods to create test data
    private JavadocPackage createTestPackage() {
        JavadocPackage pkg = new JavadocPackage();
        pkg.setId(1L);
        pkg.setLibraryName(LIBRARY);
        pkg.setVersion(VERSION);
        pkg.setPackageName(PACKAGE_NAME);
        pkg.setSummary("Chat API package");
        pkg.setDescription("Full description of chat package");
        pkg.setSourceUrl("https://docs.spring.io/spring-ai/docs/1.1.1/api/");
        return pkg;
    }

    private JavadocClass createTestClass(JavadocPackage pkg) {
        JavadocClass cls = new JavadocClass();
        cls.setId(1L);
        cls.setJavadocPackage(pkg);
        cls.setFqcn(CLASS_FQCN);
        cls.setSimpleName("ChatClient");
        cls.setKind(JavadocClassKind.INTERFACE);
        cls.setModifiers("public");
        cls.setSummary("Client to perform AI chat requests");
        cls.setDescription("Full description");
        cls.setInterfaces(List.of());
        cls.setAnnotations(List.of());
        cls.setSourceUrl("https://docs.spring.io/spring-ai/docs/1.1.1/api/ChatClient.html");

        // Add a method
        JavadocMethod method = new JavadocMethod();
        method.setId(1L);
        method.setName("call");
        method.setSignature("String call(String prompt)");
        method.setReturnType("String");
        method.setSummary("Call the AI");
        method.setJavadocClass(cls);
        method.setParameters(List.of(
                Map.of("name", "prompt", "type", "String", "description", "The prompt")
        ));
        method.setThrowsList(List.of());
        method.setAnnotations(List.of());
        cls.setMethods(new HashSet<>(Set.of(method)));

        // Add a field
        JavadocField field = new JavadocField();
        field.setId(1L);
        field.setName("DEFAULT_TIMEOUT");
        field.setType("long");
        field.setModifiers("public static final");
        field.setSummary("Default timeout in ms");
        field.setJavadocClass(cls);
        cls.setFields(new HashSet<>(Set.of(field)));

        // Add a constructor
        JavadocConstructor constructor = new JavadocConstructor();
        constructor.setId(1L);
        constructor.setSignature("ChatClient()");
        constructor.setSummary("Default constructor");
        constructor.setJavadocClass(cls);
        constructor.setParameters(List.of());
        constructor.setThrowsList(List.of());
        constructor.setAnnotations(List.of());
        cls.setConstructors(new HashSet<>(Set.of(constructor)));

        return cls;
    }
}

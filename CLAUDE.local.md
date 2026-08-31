# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

### Build
```bash
./gradlew build
```

### Run the application
```bash
./gradlew run --args="<file|directory>..."
```

### Run tests (using JUnit 5 Platform)
```bash
./gradlew test
```

### Create executable JAR
```bash
./gradlew bootJar
```

### Run the JAR file
```bash
java -jar build/libs/java-class-scanner-*.jar [options] <file|directory>...
```

### Run the demo
```bash
./demo.sh
```
Requires `./gradlew bootJar` to have been run first. Compiles the small sample project under `demo/src/com/example/library` (a plain `javac` build, no Gradle involved) into `demo/build/` and runs the tool against it, exercising most CLI options end-to-end with visible output. `demo/build/` is gitignored (covered by the existing `build/` rule) and gets recreated on every run.

## Project Architecture

This is a Spring Boot command-line application that scans Java class files and directories to extract class information and output it in various formats.

### Core Components

- **Main.java**: Entry point that sets up Spring Boot application context and handles exit codes
- **ClassScannerRunner.java**: Thin CLI orchestrator implementing `ApplicationRunner` and `ExitCodeGenerator`. Parses arguments, drives the scan loop, and delegates extraction/formatting to the components below (DI-injected).
- **model/{ClassRecord,MethodRecord,FieldRecord,ConstructorRecord}.java**: Immutable Java records (DTOs) shared between extraction and formatting. Multi-value fields (interfaces, parameters, annotations) are held as `List`/`List<List<...>>`, not pre-joined strings.
- **extract/RecordExtractor.java** (`@Component`): Converts ClassGraph's `ClassInfo`/`MethodInfo`/`FieldInfo`/`ConstructorInfo` into the DTOs above, applying package filtering, method filtering, and sorting.
- **output/RecordWriter.java** (interface) + **CsvRecordWriter/JsonRecordWriter/YamlRecordWriter** (`@Component`, Strategy pattern): Serialize DTO lists to CSV/TSV/JSON/YAML respectively, selected at runtime by `ClassScannerRunner` based on `--format`.
- **config/JacksonConfig.java** (`@Configuration`): Defines the `jsonMapper` and `yamlMapper` beans. A dedicated `jsonMapper` bean is required — see "Jackson 3 gotcha" below.

### Key Technologies

- **Spring Boot 4.1.1** with Java 25
- **ClassGraph 4.8.194**: Primary library for scanning and analyzing Java classes
- **Apache Commons CSV 1.14.1**: For CSV/TSV output generation
- **Jackson 3.x** (`tools.jackson`, not the legacy `com.fasterxml.jackson`): For JSON/YAML output generation. Version is managed via Spring Boot's BOM (no explicit version in `build.gradle`).
- **JSpecify** (`org.jspecify`): Null-safety annotations (`@NullMarked` per package, `@Nullable` for exceptions). Version managed via Spring Boot's BOM. See "Code Quality Patterns" below.
- **jqwik 1.10.1**: Property-Based Testing framework (test scope only)
- **Gradle**: Build system with Spring Boot plugin

### Application Flow

1. Parse command-line arguments through Spring Boot's `ApplicationArguments`
2. Validate input files/directories exist
3. Resolve output format (`csv`/`tsv`/`json`/`yaml`, invalid values fall back to `csv` with a warning) and charset once for the whole run
4. Use ClassGraph to scan each specified JAR file or directory
5. Apply package filtering if specified, via `RecordExtractor`
6. Extract class information (classes, methods, fields, constructors) into DTOs via `RecordExtractor`
7. Output results to console and/or CSV/TSV/JSON/YAML files:
   - **CSV/TSV**: written incrementally per input file (streaming, matches pre-existing behavior)
   - **JSON/YAML**: accumulated across all input files in memory, then written once as a single flat array/sequence after the scan loop completes (see "CSV vs JSON/YAML write timing" below)

### Output Formats

The application supports multiple output modes:
- Console output (standard or verbose — verbose also shows class modifiers and class-level annotations)
- Classes CSV/TSV/JSON/YAML export (`--classes-output`)
- Methods CSV/TSV/JSON/YAML export (`--methods-output`)
- Fields CSV/TSV/JSON/YAML export (`--fields-output`)
- Constructors CSV/TSV/JSON/YAML export (`--constructors-output`)

### Configuration

- **application.yml**: Disables Spring Boot banner, configures logging levels (switched from `application.properties`)
- **Charset support**: UTF-8 default with customizable encoding for output files
- **Package filtering**: Optional filtering by package name patterns

### Command-line Options

- `--verbose`: Show detailed class information (including modifiers and class annotations)
- `--package=<package>`: Filter by package name
- `--classes-output=<file>`: Output classes to file
- `--methods-output=<file>`: Output methods to file
- `--fields-output=<file>`: Output fields to file
- `--constructors-output=<file>`: Output constructors to file
- `--format=<format>`: Output format (`csv`/`tsv`/`json`/`yaml`)
- `--charset=<charset>`: Character encoding
- `--quiet`: Suppress standard output

**Note**: The old `--methods-csv`/`--fields-csv`/`--constructors-csv` option names were removed (no deprecation period, no backward compatibility) in favor of the `-output` naming above, since the underlying formats are no longer CSV-only.

## Architecture Details

### Unified write timing (all formats aggregate-then-write-once)
`ClassScannerRunner.processFile()` returns an immutable `Aggregation` record holding that single file's extracted DTOs (empty lists for kinds not requested). `processJarFiles()` folds these together across the scan loop via `Aggregation.merge()` (`running = running.merge(processFile(...))`) rather than passing a mutable collector down and having `processFile` add to it — returning a value and merging at the call site reads more clearly than a side-effecting out-parameter. After the loop, the selected `RecordWriter` (`CsvRecordWriter`/`JsonRecordWriter`/`YamlRecordWriter`) is called exactly once per output kind, in a `finally` block — so a best-effort write still happens even if a later input file fails mid-loop (BR-12). An empty result still produces a file (CSV/TSV: header only; JSON/YAML: `[]`) rather than no file at all (BR-7).

**History**: CSV/TSV originally wrote incrementally per input file (streaming, matching the pre-refactor implementation), while only JSON/YAML used this aggregate-then-write-once model — a JSON array/YAML sequence can't be "appended to" like a text file. This asymmetry was deliberately removed post-Code-Generation at the user's request, trading away CSV/TSV's low-memory streaming characteristic for a simpler, single code path (`RecordWriter.write()` no longer takes an `append` parameter at all). See `functional-design/business-rules.md` BR-8/BR-9 for the full rationale.

### Output Column/Key Structure
All outputs include a source path column/key first. CSV/TSV headers are Japanese; JSON/YAML keys are English camelCase (same field, different naming convention — see README.md for the full mapping table):
- **Classes**: `ソースパス, クラス名, 型, 親クラス, 実装インターフェース, パッケージ, 修飾子, クラスアノテーション`
- **Methods**: `ソースパス, クラス名, メソッド名, 返却値, 引数, 修飾子, IsStatic, メソッドアノテーション, 引数アノテーション`
- **Fields**: `ソースパス, クラス名, フィールド名, フィールド型, 修飾子, IsStatic, フィールドアノテーション`
- **Constructors**: `ソースパス, クラス名, 引数, 修飾子, コンストラクタアノテーション, 引数アノテーション`

Multi-value fields (interfaces, parameters, annotations) are delimiter-joined into a single CSV/TSV column (`, ` between items; parameter-annotations use a double delimiter: `;` within a parameter's annotations, ` | ` between parameters) but are native arrays in JSON/YAML. A field with no value (e.g. `superclass` for an interface) is an empty string in CSV/TSV and an explicit `null` in JSON/YAML — never an omitted key.

### Jackson 3 gotcha: don't let Spring autowire `ObjectMapper` by type
`tools.jackson.dataformat.yaml.YAMLMapper` **extends** `tools.jackson.databind.ObjectMapper`. If `JsonRecordWriter` declares a plain `ObjectMapper` constructor parameter and relies on Spring's default auto-configured bean, Spring's type-based autowiring can resolve it to the `yamlMapper` bean instead (both are type-compatible), silently producing YAML output when JSON was requested. This was caught via manual smoke testing, not by the compiler. Fix: `JacksonConfig` defines an explicit `jsonMapper` bean, and `JsonRecordWriter`'s constructor uses `@Qualifier("jsonMapper")`. If you ever add another `ObjectMapper`-typed bean, keep using explicit `@Qualifier`s rather than relying on type resolution.

### Logging Configuration
- **Message-only format**: `logging.pattern.console=%msg%n` (no timestamps or levels shown)
- **ClassScannerRunner**: INFO level for application output
- **Framework components**: WARN level to reduce noise
- Uses SLF4J with logger instance obtained via `getClass()`

### Key Implementation Patterns
- **Dependency injection**: `ClassScannerRunner` receives `RecordExtractor` and all three `RecordWriter` implementations via constructor injection (Spring-managed singletons), not manual instantiation
- **Strategy pattern**: `RecordWriter<T>` is a common interface; `ClassScannerRunner` picks the concrete implementation (`CsvRecordWriter`/`JsonRecordWriter`/`YamlRecordWriter`) at runtime based on `--format`. Generic type erasure means the injected fields are typed `CsvRecordWriter<?>` etc.; a small `writeRecords()` helper does the one unchecked cast in one place
- **Records as DTOs**: `model` package uses Java 25 `record` types exclusively — immutable, no behavior, just data
- **Type token for empty-list headers**: `RecordWriter.write()` takes a `Class<T> type` parameter so `CsvRecordWriter` can resolve the right header row even when `records` is empty (BR-7); JSON/YAML writers ignore it since Jackson serializes from the instances directly
- **Method references**: Uses `Comparator.comparing()`, `MethodParameterInfo::getTypeSignatureOrTypeDescriptor`, and `AnnotationInfo::getName` for clean code
- **Stream processing**: Leverages `Stream.of()` consistently instead of `Arrays.stream()` with `toList()` instead of `Collectors.toList()`
- **Annotation handling**: `RecordExtractor` returns raw `List<String>`/`List<List<String>>` for annotations; only `CsvRecordWriter` joins them into delimited strings (JSON/YAML keep them as arrays)
- **Resource management**: Proper try-with-resources for file operations
- **Java 25 features**: Uses `getFirst()` method for list access, records, and pattern-matching `switch` (e.g. `CsvRecordWriter.rowValues()` switches on the DTO's runtime type)
- **Method filtering**: `RecordExtractor.isRegularMethod()` (private) filters out constructors, static initializers, and lambda methods
- **Comprehensive sorting**: All outputs are sorted (classes by name; methods/fields by name within each class; constructors by parameter count within each class — sorting is per-class, not globally across all scanned classes)

### Code Quality Patterns
- **DRY principle**: Eliminated duplicate Stream processing through method extraction
- **Single responsibility**: Extraction (`RecordExtractor`), formatting (`RecordWriter` implementations), and orchestration (`ClassScannerRunner`) are separate components, each independently testable
- **Null safety**: JSpecify (`org.jspecify:jspecify`, version managed by the Spring Boot BOM). Every `cherry.classscanner*` package has a `package-info.java` annotated `@NullMarked`, so non-null is the default and only genuinely nullable elements carry `@Nullable` (currently just `ClassRecord.superclass` and the two `packageFilter` parameters in `RecordExtractor`). Do not write `@Nonnull` — it would be redundant noise under `@NullMarked` and there's deliberately none left in the codebase. When adding a new package under `cherry.classscanner`, add a matching `package-info.java` with `@NullMarked`. Note: `jakarta.annotation-api` is still a `testImplementation`-only dependency, but only because the `fixtures/SampleClass` test fixture uses `jakarta.annotation.Nonnull` as a stand-in runtime-visible annotation to verify ClassGraph's annotation extraction — it has nothing to do with this project's own null-safety convention.
- **Error handling**: Graceful fallbacks with user warnings for invalid inputs (charset, format — now covering `csv`/`tsv`/`json`/`yaml`)
- **Immutable collections**: Uses `toList()` for immutable result collections
- **Testing**: JUnit 5 + AssertJ for example-based tests; jqwik for property-based tests (`*PropertyTest.java`, separated by naming from example-based tests per project convention). Tests scan real fixture classes (`src/test/java/cherry/classscanner/fixtures/`) via ClassGraph rather than mocking ClassGraph's `ClassInfo`/`MethodInfo` types, which are impractical to construct synthetically
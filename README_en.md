# Java Class Scanner

Java Class Scanner is a command-line tool that analyzes Java class files in JAR files and directories, extracting and outputting class information.

## Features

- Extract detailed information from Java classes in JAR files and directories
- CSV/TSV/JSON/YAML output for classes, methods, fields, and constructors information
- Annotation information extraction and output support (classes, methods, fields, constructors, parameter annotations)
- Result aggregation for multiple file scanning (with source path tracking)
- Sorted output in alphabetical order and parameter count order
- Package name filtering functionality
- Multiple character encoding support
- Detailed display mode (including class modifiers and annotations)

## Requirements

- Java 25 or higher
- Gradle 9.x (for building)

## Build

```bash
./gradlew build
```

Create executable JAR file:
```bash
./gradlew bootJar
```

## Demo

After running `./gradlew bootJar`, you can run the demo:

```bash
./demo.sh
```

It compiles a small sample project (a library domain model) under `demo/src` on the fly, then runs through the tool's main features against it — console output, CSV/TSV/JSON/YAML output, multi-input aggregation, and invalid-value fallbacks — showing the results of each.

## Usage

### Basic Usage

```bash
java -jar build/libs/java-class-scanner-*.jar <file|directory>...
```

### Options

| Option | Description |
|--------|-------------|
| `--verbose` | Show detailed class information |
| `--package=<package>` | Filter by specified package name |
| `--classes-output=<file>` | Output class list to file |
| `--methods-output=<file>` | Output method information to file |
| `--fields-output=<file>` | Output field information to file |
| `--constructors-output=<file>` | Output constructor information to file |
| `--format=<format>` | Output format (csv, tsv, json, or yaml; default: csv) |
| `--charset=<charset>` | Character encoding for output files (default: UTF-8) |
| `--quiet` | Suppress standard output |

### Usage Examples

1. Basic class list display for JAR file:
```bash
java -jar java-class-scanner.jar myapp.jar
```

2. Display class information with detailed information:
```bash
java -jar java-class-scanner.jar --verbose myapp.jar
```

3. Display only classes from specific package:
```bash
java -jar java-class-scanner.jar --package=com.example myapp.jar
```

4. Output method information to CSV file:
```bash
java -jar java-class-scanner.jar --methods-output=methods.csv myapp.jar
```

5. Combine multiple output options:
```bash
java -jar java-class-scanner.jar \
  --classes-output=classes.tsv \
  --methods-output=methods.tsv \
  --fields-output=fields.tsv \
  --constructors-output=constructors.tsv \
  --format=tsv \
  --charset=Shift_JIS \
  myapp.jar
```

6. Analyze class files in directory:
```bash
java -jar java-class-scanner.jar /path/to/classes
```

7. Batch analysis of multiple JAR files and aggregate to CSV:
```bash
java -jar java-class-scanner.jar --methods-output=all-methods.csv app1.jar app2.jar lib.jar
```

8. Output class list and method information as JSON:
```bash
java -jar java-class-scanner.jar \
  --classes-output=classes.json \
  --methods-output=methods.json \
  --format=json \
  myapp.jar
```

9. Output as YAML:
```bash
java -jar java-class-scanner.jar --methods-output=methods.yaml --format=yaml myapp.jar
```

## Output Formats

CSV/TSV output columns and JSON/YAML output keys (English camelCase) correspond to each other.
Fields with multiple values (implemented interfaces, parameters, various annotations, etc.) are output
as a single delimiter-separated column in CSV/TSV, but as native arrays in JSON/YAML (parameter
annotations become an array of arrays). Fields with no value (e.g. a class's superclass) are output
as an empty string in CSV/TSV, and as an explicit `null` in JSON/YAML.

### Class Information (classes.csv)
| Column (CSV) | Key (JSON/YAML) | Description |
|--------|--------|-------------|
| ソースパス | sourcePath | Scanned file/directory path |
| クラス名 | className | Full class name |
| 型 | type | Class / Interface / Abstract Class / Enum / Annotation |
| 親クラス | superclass | Superclass name (empty/null if none) |
| 実装インターフェース | interfaces | Implemented interfaces |
| パッケージ | packageName | Package name |
| 修飾子 | modifiers | Access modifiers etc. |
| クラスアノテーション | classAnnotations | Annotations applied to the class |

### Method Information (methods.csv)
| Column (CSV) | Key (JSON/YAML) | Description |
|--------|--------|-------------|
| ソースパス | sourcePath | Scanned file/directory path |
| クラス名 | className | Full class name |
| メソッド名 | methodName | Method name |
| 返却値 | returnType | Return value type |
| 引数 | parameters | Parameter type list |
| 修飾子 | modifiers | Access modifiers etc. |
| IsStatic | isStatic | Whether it's a static method |
| メソッドアノテーション | methodAnnotations | Annotations applied to the method |
| 引数アノテーション | parameterAnnotations | Annotations applied to each parameter |

### Field Information (fields.csv)
| Column (CSV) | Key (JSON/YAML) | Description |
|--------|--------|-------------|
| ソースパス | sourcePath | Scanned file/directory path |
| クラス名 | className | Full class name |
| フィールド名 | fieldName | Field name |
| フィールド型 | fieldType | Field type |
| 修飾子 | modifiers | Access modifiers etc. |
| IsStatic | isStatic | Whether it's a static field |
| フィールドアノテーション | fieldAnnotations | Annotations applied to the field |

### Constructor Information (constructors.csv)
| Column (CSV) | Key (JSON/YAML) | Description |
|--------|--------|-------------|
| ソースパス | sourcePath | Scanned file/directory path |
| クラス名 | className | Full class name |
| 引数 | parameters | Parameter type list |
| 修飾子 | modifiers | Access modifiers etc. |
| コンストラクタアノテーション | constructorAnnotations | Annotations applied to the constructor |
| 引数アノテーション | parameterAnnotations | Annotations applied to each parameter |

### Notes
- When multiple files/directories are specified, all results are aggregated into a single output file (regardless of format, written once as a whole after all inputs are processed)
- The source path column/key allows tracking which file/directory each class was extracted from
- Annotation information includes fully qualified class names
- Output is automatically sorted (class names, method names, field names: alphabetical order, constructors: parameter count order)
- Internal methods (`<init>`, `<clinit>`, lambda methods) are excluded from output
- An output file is generated even when there are zero matching records (CSV/TSV: header only, JSON/YAML: empty array/sequence)

## Development

### Run Tests
```bash
./gradlew test
```

### Development Execution
```bash
./gradlew run --args="<arguments>"
```

## Technical Specifications

- **Framework**: Spring Boot 4.1.0
- **Java Version**: Java 25
- **Key Libraries**:
  - ClassGraph 4.8.184 (class analysis)
  - Apache Commons CSV 1.14.1 (CSV/TSV output)
  - Jackson 3.x (`tools.jackson`, JSON/YAML output)
  - Apache Commons Lang3 (utilities)
  - jqwik 1.10.1 (Property-Based Testing, test scope only)

### Architecture Features
- **Modern Java Features**: Utilizes Java 25's `toList()`, record types, method references, and switch expressions
- **Extraction/Formatting Separation**: An extraction layer converts ClassGraph data into DTOs (records), and a formatting layer (Strategy pattern) converts DTOs into CSV/TSV/JSON/YAML
- **Spring DI-based Composition**: Components are assembled as Spring-managed beans via constructor injection
- **Aggregate Write**: All formats write once, after all inputs are fully processed
- **Comprehensive Sorting**: Consistent sorting across all outputs

## License

Apache License 2.0
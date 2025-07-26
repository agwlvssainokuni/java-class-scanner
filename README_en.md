# Java Class Scanner

Java Class Scanner is a command-line tool that analyzes Java class files in JAR files and directories, extracting and outputting class information.

## Features

- Extract detailed information from Java classes in JAR files and directories
- CSV/TSV output for methods, fields, and constructors information
- Annotation information extraction and output support (methods, fields, constructors, parameter annotations)
- Result aggregation for multiple file scanning (with source path tracking)
- Sorted output in alphabetical order and parameter count order
- Package name filtering functionality
- Multiple character encoding support
- Detailed display mode

## Requirements

- Java 21 or higher
- Gradle 8.x (for building)

## Build

```bash
./gradlew build
```

Create executable JAR file:
```bash
./gradlew bootJar
```

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
| `--methods-csv=<file>` | Output method information to CSV file |
| `--fields-csv=<file>` | Output field information to CSV file |
| `--constructors-csv=<file>` | Output constructor information to CSV file |
| `--format=<format>` | Output format (csv or tsv, default: csv) |
| `--charset=<charset>` | Character encoding for CSV files (default: UTF-8) |
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
java -jar java-class-scanner.jar --methods-csv=methods.csv myapp.jar
```

5. Combine multiple output options:
```bash
java -jar java-class-scanner.jar \
  --methods-csv=methods.csv \
  --fields-csv=fields.csv \
  --constructors-csv=constructors.csv \
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
java -jar java-class-scanner.jar --methods-csv=all-methods.csv app1.jar app2.jar lib.jar
```

## CSV Output Format

### Method Information (methods.csv)
| Column | Description |
|--------|-------------|
| SourcePath | Scanned file/directory path |
| ClassName | Full class name |
| MethodName | Method name |
| ReturnType | Return value type |
| Parameters | Parameter type list |
| Modifiers | Access modifiers etc. |
| IsStatic | Whether it's a static method |
| MethodAnnotations | Annotations applied to the method |
| ParameterAnnotations | Annotations applied to each parameter |

### Field Information (fields.csv)
| Column | Description |
|--------|-------------|
| SourcePath | Scanned file/directory path |
| ClassName | Full class name |
| FieldName | Field name |
| FieldType | Field type |
| Modifiers | Access modifiers etc. |
| IsStatic | Whether it's a static field |
| FieldAnnotations | Annotations applied to the field |

### Constructor Information (constructors.csv)
| Column | Description |
|--------|-------------|
| SourcePath | Scanned file/directory path |
| ClassName | Full class name |
| Parameters | Parameter type list |
| Modifiers | Access modifiers etc. |
| ConstructorAnnotations | Annotations applied to the constructor |
| ParameterAnnotations | Annotations applied to each parameter |

### Notes
- When multiple files/directories are specified, all results are aggregated into a single CSV file
- The source path column allows tracking which file/directory each class was extracted from
- Annotation information includes fully qualified class names
- Output is automatically sorted (class names, method names, field names: alphabetical order, constructors: parameter count order)
- Internal methods (`<init>`, `<clinit>`, lambda methods) are excluded from output

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

- **Framework**: Spring Boot 3.5.4
- **Java Version**: Java 21
- **Key Libraries**:
  - ClassGraph 4.8.165 (class analysis)
  - Apache Commons CSV 1.10.0 (CSV output)
  - Apache Commons Lang3 (utilities)

### Architecture Features
- **Modern Java Features**: Utilizes Java 21's `toList()`, method references, and switch expressions
- **Code Quality**: Helper method extraction based on DRY principles
- **CSV Output Optimization**: Efficient file processing with header management and append mode
- **Comprehensive Sorting**: Consistent sorting across all outputs

## License

Apache License 2.0
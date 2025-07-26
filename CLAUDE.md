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

## Project Architecture

This is a Spring Boot command-line application that scans Java class files and directories to extract class information and output it in various formats.

### Core Components

- **Main.java**: Entry point that sets up Spring Boot application context and handles exit codes
- **ClassScannerRunner.java**: Main application logic implementing `ApplicationRunner` and `ExitCodeGenerator`

### Key Technologies

- **Spring Boot 3.5.4** with Java 21
- **ClassGraph 4.8.165**: Primary library for scanning and analyzing Java classes
- **Apache Commons CSV 1.10.0**: For CSV/TSV output generation
- **Gradle**: Build system with Spring Boot plugin

### Application Flow

1. Parse command-line arguments through Spring Boot's `ApplicationArguments`
2. Validate input files/directories exist
3. Use ClassGraph to scan each specified JAR file or directory
4. Apply package filtering if specified
5. Extract class information (methods, fields, constructors)
6. Output results to console and/or CSV/TSV files

### Output Formats

The application supports multiple output modes:
- Console output (standard or verbose)
- Methods CSV/TSV export
- Fields CSV/TSV export  
- Constructors CSV/TSV export

### Configuration

- **application.properties**: Disables Spring Boot banner, configures logging levels
- **Charset support**: UTF-8 default with customizable encoding for CSV output
- **Package filtering**: Optional filtering by package name patterns

### Command-line Options

- `--verbose`: Show detailed class information
- `--package=<package>`: Filter by package name
- `--methods-csv=<file>`: Output methods to CSV
- `--fields-csv=<file>`: Output fields to CSV
- `--constructors-csv=<file>`: Output constructors to CSV
- `--format=<format>`: Output format (csv/tsv)
- `--charset=<charset>`: Character encoding
- `--quiet`: Suppress standard output
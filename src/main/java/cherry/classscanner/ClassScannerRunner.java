/*
 * Copyright 2025 agwlvssainokuni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cherry.classscanner;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ClassScannerRunner implements ApplicationRunner, ExitCodeGenerator {

    private int exitCode = 0;

    @Override
    public void run(@Nonnull ApplicationArguments args) throws IOException {
        if (args.getNonOptionArgs().isEmpty()) {
            if (!args.containsOption("quiet")) {
                System.out.println("Usage: java -jar class-scanner.jar [options] <file|directory>...");
                System.out.println("Options:");
                System.out.println("  --verbose              Show detailed class information");
                System.out.println("  --package=<package>    Filter by package name");
                System.out.println("  --methods-csv=<file>   Output methods to CSV file");
                System.out.println("  --fields-csv=<file>    Output fields to CSV file");
                System.out.println("  --constructors-csv=<file> Output constructors to CSV file");
                System.out.println("  --format=<format>      Output format: csv or tsv (default: csv)");
                System.out.println("  --charset=<charset>    Character encoding for CSV files (default: UTF-8)");
                System.out.println("  --quiet                Suppress standard output");
            }
            exitCode = 0;
            return;
        }

        try {
            processJarFiles(args);
            exitCode = 0;
        } catch (IOException e) {
            if (!args.containsOption("quiet")) {
                System.out.println("Error processing files: " + e.getMessage());
            }
            exitCode = 1;
        }
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

    private void processJarFiles(
            @Nonnull ApplicationArguments args
    ) throws IOException {
        var files = findProcessableFiles(args.getNonOptionArgs());

        if (files.isEmpty()) {
            if (!args.containsOption("quiet")) {
                System.out.println("No processable files or directories found in arguments.");
            }
            return;
        }

        for (String filePath : files) {
            processFile(filePath, args);
        }
    }

    @Nonnull
    private List<String> findProcessableFiles(
            @Nonnull List<String> nonOptionArgs
    ) {
        return nonOptionArgs.stream()
                .filter(arg -> {
                    Path path = Paths.get(arg);
                    return Files.exists(path) && (Files.isRegularFile(path) || Files.isDirectory(path));
                })
                .collect(Collectors.toList());
    }

    private void processFile(
            @Nonnull String filePath,
            @Nonnull ApplicationArguments args
    ) throws IOException {
        var quiet = args.containsOption("quiet");
        Path path = Paths.get(filePath);
        var isDirectory = Files.isDirectory(path);

        if (!quiet) {
            System.out.println("\n=== Analyzing " + (isDirectory ? "directory" : "file") + ": " + filePath + " ===");
        }

        // Create a clean ClassGraph instance that only scans the specified file
        try (ScanResult scanResult = new ClassGraph()
                .overrideClasspath(filePath)  // Only scan this file
                .removeTemporaryFilesAfterScan()
                .enableAllInfo()
                .scan()) {

            var allClasses = scanResult.getAllClasses();

            if (allClasses.isEmpty()) {
                if (!quiet) {
                    System.out.println("No classes found in " + (Files.isDirectory(Paths.get(filePath)) ? "directory." : "file."));
                }
                return;
            }

            var packageFilter = args.containsOption("package") ?
                    args.getOptionValues("package") : null;

            var filteredClasses = allClasses.stream()
                    .filter(classInfo -> matchesPackageFilter(classInfo.getName(), packageFilter))
                    .sorted((a, b) -> a.getName().compareTo(b.getName()))
                    .collect(Collectors.toList());

            if (!quiet) {
                System.out.println("Found " + filteredClasses.size() + " classes:");
            }

            // CSV/TSV output
            var format = args.containsOption("format") ?
                    args.getOptionValues("format").get(0).toLowerCase() : "csv";
            var charsetName = args.containsOption("charset") ?
                    args.getOptionValues("charset").get(0) : "UTF-8";

            if (args.containsOption("methods-csv")) {
                var methodsFile = args.getOptionValues("methods-csv").get(0);
                outputMethodsToCsv(filteredClasses, methodsFile, format, charsetName, quiet);
            }

            if (args.containsOption("fields-csv")) {
                var fieldsFile = args.getOptionValues("fields-csv").get(0);
                outputFieldsToCsv(filteredClasses, fieldsFile, format, charsetName, quiet);
            }

            if (args.containsOption("constructors-csv")) {
                var constructorsFile = args.getOptionValues("constructors-csv").get(0);
                outputConstructorsToCsv(filteredClasses, constructorsFile, format, charsetName, quiet);
            }

            // Standard output
            if (!quiet) {
                var verbose = args.containsOption("verbose");
                filteredClasses.forEach(classInfo -> {
                    if (verbose) {
                        printVerboseClassInfo(classInfo);
                    } else {
                        System.out.println("  " + classInfo.getName());
                    }
                });
            }
        }
    }

    private void outputMethodsToCsv(
            @Nonnull List<ClassInfo> classes,
            @Nonnull String fileName,
            @Nonnull String format,
            @Nonnull String charsetName,
            boolean quiet
    ) throws IOException {
        var charset = getCharset(charsetName, quiet);

        var csvFormat = getCSVFormat(format)
                .builder()
                .setHeader("クラス名", "メソッド名", "返却値", "引数", "修飾子", "IsStatic")
                .build();

        try (FileWriter writer = new FileWriter(fileName, charset);
             CSVPrinter printer = new CSVPrinter(writer, csvFormat)) {

            for (ClassInfo classInfo : classes) {
                var methods = classInfo.getMethodInfo();
                for (var methodInfo : methods) {
                    if (!methodInfo.getName().equals("<init>") &&
                            !methodInfo.getName().equals("<clinit>") &&
                            !methodInfo.getName().contains("lambda$")) {

                        var returnType = methodInfo.getTypeSignatureOrTypeDescriptor().getResultType().toString();
                        var parameters = methodInfo.getParameterInfo().length > 0 ?
                                java.util.Arrays.stream(methodInfo.getParameterInfo())
                                        .map(param -> param.getTypeSignatureOrTypeDescriptor().toString())
                                        .collect(Collectors.joining(", ")) : "";

                        printer.printRecord(
                                classInfo.getName(),
                                methodInfo.getName(),
                                returnType,
                                parameters,
                                methodInfo.getModifiersStr(),
                                methodInfo.isStatic()
                        );
                    }
                }
            }
        }

        if (!quiet) {
            System.out.println("Methods " + format.toUpperCase() + " generated: " + fileName + " (encoding: " + charset + ")");
        }
    }

    private void outputFieldsToCsv(
            @Nonnull List<ClassInfo> classes,
            @Nonnull String fileName,
            @Nonnull String format,
            @Nonnull String charsetName,
            boolean quiet
    ) throws IOException {
        var charset = getCharset(charsetName, quiet);

        var csvFormat = getCSVFormat(format)
                .builder()
                .setHeader("クラス名", "フィールド名", "フィールド型", "修飾子", "IsStatic")
                .build();

        try (FileWriter writer = new FileWriter(fileName, charset);
             CSVPrinter printer = new CSVPrinter(writer, csvFormat)) {

            for (ClassInfo classInfo : classes) {
                var fields = classInfo.getFieldInfo();
                for (var fieldInfo : fields) {
                    var fieldType = fieldInfo.getTypeSignatureOrTypeDescriptor().toString();

                    printer.printRecord(
                            classInfo.getName(),
                            fieldInfo.getName(),
                            fieldType,
                            fieldInfo.getModifiersStr(),
                            fieldInfo.isStatic()
                    );
                }
            }
        }

        if (!quiet) {
            System.out.println("Fields " + format.toUpperCase() + " generated: " + fileName + " (encoding: " + charset + ")");
        }
    }

    private void outputConstructorsToCsv(
            @Nonnull List<ClassInfo> classes,
            @Nonnull String fileName,
            @Nonnull String format,
            @Nonnull String charsetName,
            boolean quiet
    ) throws IOException {
        var charset = getCharset(charsetName, quiet);

        var csvFormat = getCSVFormat(format)
                .builder()
                .setHeader("クラス名", "引数", "修飾子")
                .build();

        try (FileWriter writer = new FileWriter(fileName, charset);
             CSVPrinter printer = new CSVPrinter(writer, csvFormat)) {

            for (ClassInfo classInfo : classes) {
                var constructors = classInfo.getConstructorInfo();
                for (var constructorInfo : constructors) {
                    var parameters = constructorInfo.getParameterInfo().length > 0 ?
                            java.util.Arrays.stream(constructorInfo.getParameterInfo())
                                    .map(param -> param.getTypeSignatureOrTypeDescriptor().toString())
                                    .collect(Collectors.joining(", ")) : "";

                    printer.printRecord(
                            classInfo.getName(),
                            parameters,
                            constructorInfo.getModifiersStr()
                    );
                }
            }
        }

        if (!quiet) {
            System.out.println("Constructors " + format.toUpperCase() + " generated: " + fileName + " (encoding: " + charset + ")");
        }
    }

    @Nonnull
    private CSVFormat getCSVFormat(@Nonnull String format) {
        return switch (format.toLowerCase()) {
            case "tsv" -> CSVFormat.TDF;
            case "csv" -> CSVFormat.DEFAULT;
            default -> {
                System.out.println("Warning: Unknown format '" + format + "', using CSV");
                yield CSVFormat.DEFAULT;
            }
        };
    }

    @Nonnull
    private Charset getCharset(@Nonnull String charsetName, boolean quiet) {
        try {
            return Charset.forName(charsetName);
        } catch (UnsupportedCharsetException | IllegalCharsetNameException e) {
            if (!quiet) {
                System.out.println("Warning: Invalid charset '" + charsetName + "', using UTF-8");
            }
            return StandardCharsets.UTF_8;
        }
    }

    private boolean matchesPackageFilter(
            @Nonnull String className,
            @Nullable List<String> packageFilter
    ) {
        if (packageFilter == null) {
            return true;
        }
        for (var pkg : packageFilter) {
            pkg = StringUtils.trim(pkg);
            if (className.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }

    private void printVerboseClassInfo(
            @Nonnull ClassInfo classInfo
    ) {
        System.out.println("  " + classInfo.getName());

        if (classInfo.isInterface()) {
            System.out.println("    Type: Interface");
        } else if (classInfo.isAbstract()) {
            System.out.println("    Type: Abstract Class");
        } else if (classInfo.isEnum()) {
            System.out.println("    Type: Enum");
        } else if (classInfo.isAnnotation()) {
            System.out.println("    Type: Annotation");
        } else {
            System.out.println("    Type: Class");
        }

        if (classInfo.getSuperclass() != null) {
            System.out.println("    Superclass: " + classInfo.getSuperclass().getName());
        }

        if (!classInfo.getInterfaces().isEmpty()) {
            System.out.println("    Interfaces: " +
                    classInfo.getInterfaces().stream()
                            .map(ClassInfo::getName)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse(""));
        }

        System.out.println("    Package: " + classInfo.getPackageName());

        // Print fields (class variables and instance variables)
        var fields = classInfo.getFieldInfo();
        if (!fields.isEmpty()) {
            System.out.println("    Fields:");
            fields.stream()
                    .sorted((a, b) -> a.getName().compareTo(b.getName()))
                    .forEach(fieldInfo -> {
                        var modifiers = fieldInfo.getModifiersStr();
                        var type = fieldInfo.getTypeSignatureOrTypeDescriptor().toString();
                        var name = fieldInfo.getName();
                        var fieldType = fieldInfo.isStatic() ? "class variable" : "instance variable";
                        System.out.println("      " + modifiers + " " + type + " " + name + " (" + fieldType + ")");
                    });
        }

        // Print methods
        var methods = classInfo.getMethodInfo();
        if (!methods.isEmpty()) {
            System.out.println("    Methods:");
            methods.stream()
                    .filter(methodInfo -> !methodInfo.getName().equals("<init>") &&
                            !methodInfo.getName().equals("<clinit>") &&
                            !methodInfo.getName().contains("lambda$"))
                    .sorted((a, b) -> a.getName().compareTo(b.getName()))
                    .forEach(methodInfo -> {
                        var modifiers = methodInfo.getModifiersStr();
                        var returnType = methodInfo.getTypeSignatureOrTypeDescriptor().getResultType().toString();
                        var name = methodInfo.getName();
                        var params = Stream.of(methodInfo.getParameterInfo())
                                .map(param -> param.getTypeSignatureOrTypeDescriptor().toString())
                                .collect(Collectors.joining(", "));
                        System.out.println("      " + modifiers + " " + returnType + " " + name + "(" + params + ")");
                    });
        }

        // Print constructors
        var constructors = classInfo.getConstructorInfo();
        if (!constructors.isEmpty()) {
            System.out.println("    Constructors:");
            constructors.stream()
                    .sorted((a, b) -> Integer.compare(a.getParameterInfo().length, b.getParameterInfo().length))
                    .forEach(constructorInfo -> {
                        var modifiers = constructorInfo.getModifiersStr();
                        var params = Stream.of(constructorInfo.getParameterInfo())
                                .map(param -> param.getTypeSignatureOrTypeDescriptor().toString())
                                .collect(Collectors.joining(", "));
                        System.out.println("      " + modifiers + " " + classInfo.getSimpleName() + "(" + params + ")");
                    });
        }

        System.out.println();
    }
}

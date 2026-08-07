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

import cherry.classscanner.extract.RecordExtractor;
import cherry.classscanner.model.ClassRecord;
import cherry.classscanner.model.ConstructorRecord;
import cherry.classscanner.model.FieldRecord;
import cherry.classscanner.model.MethodRecord;
import cherry.classscanner.output.CsvRecordWriter;
import cherry.classscanner.output.JsonRecordWriter;
import cherry.classscanner.output.RecordWriter;
import cherry.classscanner.output.YamlRecordWriter;
import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class ClassScannerRunner implements ApplicationRunner, ExitCodeGenerator {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private int exitCode = 0;

    private final RecordExtractor recordExtractor;
    private final CsvRecordWriter<?> csvRecordWriter;
    private final JsonRecordWriter<?> jsonRecordWriter;
    private final YamlRecordWriter<?> yamlRecordWriter;

    // CSV/TSV(逐次書き込み)でヘッダーを書き込み済みかどうかを"出力種別:ファイル名"単位で追跡する(BR-8)
    private final Set<String> csvFilesCreated = new HashSet<>();

    public ClassScannerRunner(
            @Nonnull RecordExtractor recordExtractor,
            @Nonnull CsvRecordWriter<?> csvRecordWriter,
            @Nonnull JsonRecordWriter<?> jsonRecordWriter,
            @Nonnull YamlRecordWriter<?> yamlRecordWriter
    ) {
        this.recordExtractor = recordExtractor;
        this.csvRecordWriter = csvRecordWriter;
        this.jsonRecordWriter = jsonRecordWriter;
        this.yamlRecordWriter = yamlRecordWriter;
    }

    @Override
    public void run(@Nonnull ApplicationArguments args) {
        if (args.getNonOptionArgs().isEmpty()) {
            if (!args.containsOption("quiet")) {
                logger.info("Usage: java -jar java-class-scanner.jar [options] <file|directory>...");
                logger.info("Options:");
                logger.info("  --verbose                     Show detailed class information");
                logger.info("  --package=<package>           Filter by package name");
                logger.info("  --classes-output=<file>       Output classes to file");
                logger.info("  --methods-output=<file>       Output methods to file");
                logger.info("  --fields-output=<file>        Output fields to file");
                logger.info("  --constructors-output=<file>  Output constructors to file");
                logger.info("  --format=<format>             Output format: csv, tsv, json, or yaml (default: csv)");
                logger.info("  --charset=<charset>            Character encoding for output files (default: UTF-8)");
                logger.info("  --quiet                        Suppress standard output");
            }
            exitCode = 0;
            return;
        }

        try {
            processJarFiles(args);
            exitCode = 0;
        } catch (IOException e) {
            if (!args.containsOption("quiet")) {
                logger.error("Error processing files: {}", e.getMessage());
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
                logger.warn("No processable files or directories found in arguments.");
            }
            return;
        }

        var format = resolveFormat(args);
        var charset = resolveCharset(args);
        // JSON/YAMLは全入力処理後に1回だけ書き込む集約モード(BR-9)。CSV/TSVは対象ファイルごとの逐次書き込み。
        var aggregation = ("json".equals(format) || "yaml".equals(format)) ? Aggregation.empty() : null;

        try {
            for (String filePath : files) {
                processFile(filePath, args, format, charset, aggregation);
            }
        } finally {
            // BR-12: 途中でエラーが発生しても、それまでに蓄積されたレコードをベストエフォートで書き出す
            if (aggregation != null) {
                writeAggregated(args, format, charset, aggregation);
            }
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
                .toList();
    }

    private void processFile(
            @Nonnull String filePath,
            @Nonnull ApplicationArguments args,
            @Nonnull String format,
            @Nonnull Charset charset,
            @Nullable Aggregation aggregation
    ) throws IOException {
        var quiet = args.containsOption("quiet");
        var isDirectory = Files.isDirectory(Paths.get(filePath));

        if (!quiet) {
            logger.info("");
            logger.info("=== Analyzing {} : {} ===", (isDirectory ? "directory" : "file"), filePath);
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
                    logger.info("No classes found in {}.", (isDirectory ? "directory" : "file"));
                }
                return;
            }

            var packageFilter = args.containsOption("package") ?
                    args.getOptionValues("package") : null;
            var filteredClasses = recordExtractor.filterAndSortByPackage(allClasses, packageFilter);

            if (!quiet) {
                logger.info("Found {} classes:", filteredClasses.size());
            }

            if (args.containsOption("classes-output")) {
                var fileName = args.getOptionValues("classes-output").getFirst();
                handleOutput(recordExtractor.extractClasses(filePath, filteredClasses), ClassRecord.class,
                        "classes", fileName, format, charset,
                        aggregation == null ? null : aggregation.classes(), quiet);
            }
            if (args.containsOption("methods-output")) {
                var fileName = args.getOptionValues("methods-output").getFirst();
                handleOutput(recordExtractor.extractMethods(filePath, filteredClasses), MethodRecord.class,
                        "methods", fileName, format, charset,
                        aggregation == null ? null : aggregation.methods(), quiet);
            }
            if (args.containsOption("fields-output")) {
                var fileName = args.getOptionValues("fields-output").getFirst();
                handleOutput(recordExtractor.extractFields(filePath, filteredClasses), FieldRecord.class,
                        "fields", fileName, format, charset,
                        aggregation == null ? null : aggregation.fields(), quiet);
            }
            if (args.containsOption("constructors-output")) {
                var fileName = args.getOptionValues("constructors-output").getFirst();
                handleOutput(recordExtractor.extractConstructors(filePath, filteredClasses), ConstructorRecord.class,
                        "constructors", fileName, format, charset,
                        aggregation == null ? null : aggregation.constructors(), quiet);
            }

            // Standard output
            if (!quiet) {
                var verbose = args.containsOption("verbose");
                filteredClasses.forEach(classInfo -> {
                    if (verbose) {
                        printVerboseClassInfo(filePath, classInfo);
                    } else {
                        logger.info("  {}", classInfo.getName());
                    }
                });
            }
        }
    }

    /**
     * CSV/TSV(逐次書き込みモード、aggregationTarget=null)は即座にファイルへ書き込む。
     * JSON/YAML(集約書き込みモード)は全入力処理完了後にまとめて書き込むため、ここではメモリ上のリストへ追加するのみ(BR-9)。
     */
    private <X> void handleOutput(
            @Nonnull List<X> records,
            @Nonnull Class<X> type,
            @Nonnull String outputKey,
            @Nonnull String fileName,
            @Nonnull String format,
            @Nonnull Charset charset,
            @Nullable List<X> aggregationTarget,
            boolean quiet
    ) throws IOException {
        if (aggregationTarget != null) {
            aggregationTarget.addAll(records);
            return;
        }

        var fileKey = outputKey + ":" + fileName;
        var append = csvFilesCreated.contains(fileKey);
        if (!append) {
            csvFilesCreated.add(fileKey);
        }

        writeRecords(csvRecordWriter, records, type, format, Path.of(fileName), charset, append);

        if (!quiet) {
            var formatName = "tsv".equals(format) ? "TSV" : "CSV";
            logger.info("{} {} generated: {} (encoding: {})", outputKey, formatName, fileName, charset);
        }
    }

    private void writeAggregated(
            @Nonnull ApplicationArguments args,
            @Nonnull String format,
            @Nonnull Charset charset,
            @Nonnull Aggregation aggregation
    ) throws IOException {
        var quiet = args.containsOption("quiet");
        RecordWriter<?> writer = "yaml".equals(format) ? yamlRecordWriter : jsonRecordWriter;

        if (args.containsOption("classes-output")) {
            writeAggregatedOne(writer, aggregation.classes(), ClassRecord.class, "classes",
                    args.getOptionValues("classes-output").getFirst(), format, charset, quiet);
        }
        if (args.containsOption("methods-output")) {
            writeAggregatedOne(writer, aggregation.methods(), MethodRecord.class, "methods",
                    args.getOptionValues("methods-output").getFirst(), format, charset, quiet);
        }
        if (args.containsOption("fields-output")) {
            writeAggregatedOne(writer, aggregation.fields(), FieldRecord.class, "fields",
                    args.getOptionValues("fields-output").getFirst(), format, charset, quiet);
        }
        if (args.containsOption("constructors-output")) {
            writeAggregatedOne(writer, aggregation.constructors(), ConstructorRecord.class, "constructors",
                    args.getOptionValues("constructors-output").getFirst(), format, charset, quiet);
        }
    }

    private <X> void writeAggregatedOne(
            @Nonnull RecordWriter<?> writer,
            @Nonnull List<X> records,
            @Nonnull Class<X> type,
            @Nonnull String outputKey,
            @Nonnull String fileName,
            @Nonnull String format,
            @Nonnull Charset charset,
            boolean quiet
    ) throws IOException {
        // append=false: 集約書き込みモードは常に全体を1回で新規書き込みする(BR-9)。0件でも空配列/空シーケンスを出力する(BR-7)。
        writeRecords(writer, records, type, format, Path.of(fileName), charset, false);

        if (!quiet) {
            logger.info("{} {} generated: {} (encoding: {})", outputKey, format.toUpperCase(), fileName, charset);
        }
    }

    @SuppressWarnings("unchecked")
    private <X> void writeRecords(
            @Nonnull RecordWriter<?> writer,
            @Nonnull List<X> records,
            @Nonnull Class<X> type,
            @Nonnull String format,
            @Nonnull Path outputPath,
            @Nonnull Charset charset,
            boolean append
    ) throws IOException {
        ((RecordWriter<X>) writer).write(records, type, format, outputPath, charset, append);
    }

    @Nonnull
    private String resolveFormat(@Nonnull ApplicationArguments args) {
        var format = args.containsOption("format") ?
                args.getOptionValues("format").getFirst().toLowerCase() : "csv";
        return switch (format) {
            case "csv", "tsv", "json", "yaml" -> format;
            default -> {
                if (!args.containsOption("quiet")) {
                    logger.warn("Warning: Unknown format '{}', using CSV", format);
                }
                yield "csv";
            }
        };
    }

    @Nonnull
    private Charset resolveCharset(@Nonnull ApplicationArguments args) {
        var charsetName = args.containsOption("charset") ?
                args.getOptionValues("charset").getFirst() : "UTF-8";
        return getCharset(charsetName, args.containsOption("quiet"));
    }

    @Nonnull
    private Charset getCharset(@Nonnull String charsetName, boolean quiet) {
        try {
            return Charset.forName(charsetName);
        } catch (UnsupportedCharsetException | IllegalCharsetNameException e) {
            if (!quiet) {
                logger.warn("Warning: Invalid charset '{}', using UTF-8", charsetName);
            }
            return StandardCharsets.UTF_8;
        }
    }

    private void printVerboseClassInfo(
            @Nonnull String sourcePath,
            @Nonnull ClassInfo classInfo
    ) {
        logger.info("  {}", classInfo.getName());

        if (classInfo.isInterface()) {
            logger.info("    Type: Interface");
        } else if (classInfo.isAbstract()) {
            logger.info("    Type: Abstract Class");
        } else if (classInfo.isEnum()) {
            logger.info("    Type: Enum");
        } else if (classInfo.isAnnotation()) {
            logger.info("    Type: Annotation");
        } else {
            logger.info("    Type: Class");
        }

        if (classInfo.getSuperclass() != null) {
            logger.info("    Superclass: {}", classInfo.getSuperclass().getName());
        }

        if (!classInfo.getInterfaces().isEmpty()) {
            logger.info("    Interfaces: {}",
                    String.join(", ", classInfo.getInterfaces().stream().map(ClassInfo::getName).toList()));
        }

        logger.info("    Package: {}", classInfo.getPackageName());

        // FR-6: verbose出力に修飾子・クラスアノテーションを追加
        logger.info("    Modifiers: {}", classInfo.getModifiersStr());
        var classAnnotations = classInfo.getAnnotationInfo().stream().map(AnnotationInfo::getName).toList();
        if (!classAnnotations.isEmpty()) {
            logger.info("    Annotations: {}", String.join(", ", classAnnotations));
        }

        // Print fields (class variables and instance variables)
        if (!classInfo.getFieldInfo().isEmpty()) {
            logger.info("    Fields:");
            recordExtractor.extractFields(sourcePath, List.of(classInfo)).forEach(field -> {
                var annotationStr = field.fieldAnnotations().isEmpty() ? "" :
                        "[" + String.join(", ", field.fieldAnnotations()) + "] ";
                var fieldKind = field.isStatic() ? "class variable" : "instance variable";
                logger.info("      {}{} {} {} ({})", annotationStr, field.modifiers(), field.fieldType(),
                        field.fieldName(), fieldKind);
            });
        }

        // Print methods
        if (!classInfo.getMethodInfo().isEmpty()) {
            logger.info("    Methods:");
            recordExtractor.extractMethods(sourcePath, List.of(classInfo)).forEach(method -> {
                var annotationStr = method.methodAnnotations().isEmpty() ? "" :
                        "[" + String.join(", ", method.methodAnnotations()) + "] ";
                logger.info("      {}{} {} {}({})", annotationStr, method.modifiers(), method.returnType(),
                        method.methodName(), String.join(", ", method.parameters()));
            });
        }

        // Print constructors
        if (!classInfo.getConstructorInfo().isEmpty()) {
            logger.info("    Constructors:");
            recordExtractor.extractConstructors(sourcePath, List.of(classInfo)).forEach(constructor -> {
                var annotationStr = constructor.constructorAnnotations().isEmpty() ? "" :
                        "[" + String.join(", ", constructor.constructorAnnotations()) + "] ";
                logger.info("      {}{} {}({})", annotationStr, constructor.modifiers(), classInfo.getSimpleName(),
                        String.join(", ", constructor.parameters()));
            });
        }

        logger.info("");
    }

    /**
     * JSON/YAML集約書き込みモード(BR-9)で、全入力を横断して蓄積するレコードの保持先。
     */
    private record Aggregation(
            @Nonnull List<ClassRecord> classes,
            @Nonnull List<MethodRecord> methods,
            @Nonnull List<FieldRecord> fields,
            @Nonnull List<ConstructorRecord> constructors
    ) {
        @Nonnull
        static Aggregation empty() {
            return new Aggregation(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }
    }
}

/*
 * Copyright 2025,2026 agwlvssainokuni
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
import java.util.List;

/**
 * 全出力フォーマット(CSV/TSV/JSON/YAML)は、全入力処理後に集約リストを1回だけ書き込む単一モデルに
 * 統一されている(business-rules.md BR-8/BR-9改訂: 既存のCSV/TSV逐次書き込みとの互換性より
 * コードのシンプルさを優先するユーザー判断による)。
 */
@Component
public class ClassScannerRunner implements ApplicationRunner, ExitCodeGenerator {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private int exitCode = 0;

    private final RecordExtractor recordExtractor;
    private final CsvRecordWriter<?> csvRecordWriter;
    private final JsonRecordWriter<?> jsonRecordWriter;
    private final YamlRecordWriter<?> yamlRecordWriter;

    public ClassScannerRunner(
            RecordExtractor recordExtractor,
            CsvRecordWriter<?> csvRecordWriter,
            JsonRecordWriter<?> jsonRecordWriter,
            YamlRecordWriter<?> yamlRecordWriter
    ) {
        this.recordExtractor = recordExtractor;
        this.csvRecordWriter = csvRecordWriter;
        this.jsonRecordWriter = jsonRecordWriter;
        this.yamlRecordWriter = yamlRecordWriter;
    }

    @Override
    public void run(ApplicationArguments args) {
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
            ApplicationArguments args
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
        var aggregation = Aggregation.empty();

        try {
            for (String filePath : files) {
                aggregation = aggregation.merge(processFile(filePath, args));
            }
        } finally {
            // BR-12: 途中でエラーが発生しても、それまでに蓄積されたレコードをベストエフォートで書き出す
            writeAggregated(args, format, charset, aggregation);
        }
    }

    private List<String> findProcessableFiles(
            List<String> nonOptionArgs
    ) {
        return nonOptionArgs.stream()
                .filter(arg -> {
                    Path path = Paths.get(arg);
                    return Files.exists(path) && (Files.isRegularFile(path) || Files.isDirectory(path));
                })
                .toList();
    }

    private Aggregation processFile(
            String filePath,
            ApplicationArguments args
    ) {
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
                return Aggregation.empty();
            }

            var packageFilter = args.containsOption("package") ?
                    args.getOptionValues("package") : null;
            var filteredClasses = recordExtractor.filterAndSortByPackage(allClasses, packageFilter);

            if (!quiet) {
                logger.info("Found {} classes:", filteredClasses.size());
            }

            var classes = args.containsOption("classes-output") ?
                    recordExtractor.extractClasses(filePath, filteredClasses) : List.<ClassRecord>of();
            var methods = args.containsOption("methods-output") ?
                    recordExtractor.extractMethods(filePath, filteredClasses) : List.<MethodRecord>of();
            var fields = args.containsOption("fields-output") ?
                    recordExtractor.extractFields(filePath, filteredClasses) : List.<FieldRecord>of();
            var constructors = args.containsOption("constructors-output") ?
                    recordExtractor.extractConstructors(filePath, filteredClasses) : List.<ConstructorRecord>of();

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

            return new Aggregation(classes, methods, fields, constructors);
        }
    }

    private void writeAggregated(
            ApplicationArguments args,
            String format,
            Charset charset,
            Aggregation aggregation
    ) throws IOException {
        var quiet = args.containsOption("quiet");
        RecordWriter<?> writer = switch (format) {
            case "json" -> jsonRecordWriter;
            case "yaml" -> yamlRecordWriter;
            default -> csvRecordWriter; // csv or tsv
        };

        if (args.containsOption("classes-output")) {
            writeOne(writer, aggregation.classes(), ClassRecord.class, "classes",
                    args.getOptionValues("classes-output").getFirst(), format, charset, quiet);
        }
        if (args.containsOption("methods-output")) {
            writeOne(writer, aggregation.methods(), MethodRecord.class, "methods",
                    args.getOptionValues("methods-output").getFirst(), format, charset, quiet);
        }
        if (args.containsOption("fields-output")) {
            writeOne(writer, aggregation.fields(), FieldRecord.class, "fields",
                    args.getOptionValues("fields-output").getFirst(), format, charset, quiet);
        }
        if (args.containsOption("constructors-output")) {
            writeOne(writer, aggregation.constructors(), ConstructorRecord.class, "constructors",
                    args.getOptionValues("constructors-output").getFirst(), format, charset, quiet);
        }
    }

    private <X> void writeOne(
            RecordWriter<?> writer,
            List<X> records,
            Class<X> type,
            String outputKey,
            String fileName,
            String format,
            Charset charset,
            boolean quiet
    ) throws IOException {
        writeRecords(writer, records, type, format, Path.of(fileName), charset);

        if (!quiet) {
            logger.info("{} {} generated: {} (encoding: {})", outputKey, format.toUpperCase(), fileName, charset);
        }
    }

    // csvRecordWriter/jsonRecordWriter/yamlRecordWriterはSpring DIによりワイルドカード型(RecordWriter<?>)
    // で保持されているため、呼び出し時にXへキャストする必要がある。全ての呼び出し元がrecords/typeと
    // 同じXをwriterに渡す規約を守っている限り安全(コンパイラは規約を検証できないためunchecked警告が出る)。
    @SuppressWarnings("unchecked")
    private <X> void writeRecords(
            RecordWriter<?> writer,
            List<X> records,
            Class<X> type,
            String format,
            Path outputPath,
            Charset charset
    ) throws IOException {
        ((RecordWriter<X>) writer).write(records, type, format, outputPath, charset);
    }

    private String resolveFormat(ApplicationArguments args) {
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

    private Charset resolveCharset(ApplicationArguments args) {
        var charsetName = args.containsOption("charset") ?
                args.getOptionValues("charset").getFirst() : "UTF-8";
        return getCharset(charsetName, args.containsOption("quiet"));
    }

    private Charset getCharset(String charsetName, boolean quiet) {
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
            String sourcePath,
            ClassInfo classInfo
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
     * 1ファイル分、または全入力を横断して蓄積したレコードの集合(BR-8/BR-9)を表す不変な値オブジェクト。
     * {@code processFile}が1ファイル分の結果をこの型で返し、呼び出し元({@code processJarFiles}）が
     * {@link #merge}で積み上げる(副作用による集約ではなく、返却値の合成として表現する)。
     */
    private record Aggregation(
            List<ClassRecord> classes,
            List<MethodRecord> methods,
            List<FieldRecord> fields,
            List<ConstructorRecord> constructors
    ) {
        static Aggregation empty() {
            return new Aggregation(List.of(), List.of(), List.of(), List.of());
        }

        Aggregation merge(Aggregation other) {
            return new Aggregation(
                    concat(classes, other.classes),
                    concat(methods, other.methods),
                    concat(fields, other.fields),
                    concat(constructors, other.constructors)
            );
        }

        private static <X> List<X> concat(List<X> a, List<X> b) {
            if (a.isEmpty()) {
                return b;
            }
            if (b.isEmpty()) {
                return a;
            }
            var combined = new ArrayList<X>(a.size() + b.size());
            combined.addAll(a);
            combined.addAll(b);
            return combined;
        }
    }
}

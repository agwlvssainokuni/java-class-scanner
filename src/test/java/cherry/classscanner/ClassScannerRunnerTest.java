/*
 * Copyright 2026 agwlvssainokuni
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
import cherry.classscanner.fixtures.SampleClass;
import cherry.classscanner.output.CsvRecordWriter;
import cherry.classscanner.output.JsonRecordWriter;
import cherry.classscanner.output.YamlRecordWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.DefaultApplicationArguments;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ClassScannerRunnerのオーケストレーションをExample-basedで検証する。
 * FR-7の既存機能バックフィル(終了コード、パッケージフィルタ、フォーマット不正値フォールバック、
 * 旧オプション名の廃止)と、新機能(classes-output、json/yaml出力、複数入力集約)の両方を対象とする。
 */
class ClassScannerRunnerTest {

    private ClassScannerRunner runner;
    private Path fixturesDir;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws URISyntaxException {
        runner = new ClassScannerRunner(
                new RecordExtractor(),
                new CsvRecordWriter<>(),
                new JsonRecordWriter<>(JsonMapper.builder().build()),
                new YamlRecordWriter<>(YAMLMapper.builder().build())
        );
        fixturesDir = Path.of(SampleClass.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    @Test
    void run_noArgs_showsUsageAndExitsZero() {
        runner.run(new DefaultApplicationArguments());

        assertThat(runner.getExitCode()).isZero();
    }

    @Test
    void run_validDirectory_exitsZero() {
        runner.run(new DefaultApplicationArguments(
                "--quiet", "--package=cherry.classscanner.fixtures", fixturesDir.toString()));

        assertThat(runner.getExitCode()).isZero();
    }

    @Test
    void run_classesOutput_createsFile() {
        var output = tempDir.resolve("classes.csv");
        runner.run(new DefaultApplicationArguments(
                "--quiet", "--package=cherry.classscanner.fixtures",
                "--classes-output=" + output, fixturesDir.toString()));

        assertThat(output).exists();
        assertThat(contentOf(output)).contains("cherry.classscanner.fixtures.SampleClass");
    }

    @Test
    void run_jsonFormat_producesValidJsonArray() {
        var output = tempDir.resolve("methods.json");
        runner.run(new DefaultApplicationArguments(
                "--quiet", "--package=cherry.classscanner.fixtures", "--format=json",
                "--methods-output=" + output, fixturesDir.toString()));

        var content = contentOf(output).strip();
        assertThat(content).startsWith("[").endsWith("]");
        assertThat(content).contains("\"methodName\"");
    }

    @Test
    void run_oldCsvOptionName_isIgnored() {
        var output = tempDir.resolve("old.csv");
        runner.run(new DefaultApplicationArguments(
                "--quiet", "--methods-csv=" + output, fixturesDir.toString()));

        assertThat(output).doesNotExist();
    }

    @Test
    void run_packageFilterExcludesAll_jsonStillWritesEmptyArray() {
        var output = tempDir.resolve("methods.json");
        runner.run(new DefaultApplicationArguments(
                "--quiet", "--package=no.such.package", "--format=json",
                "--methods-output=" + output, fixturesDir.toString()));

        assertThat(contentOf(output).strip()).isEqualTo("[ ]");
    }

    @Test
    void run_packageFilterExcludesAll_csvStillWritesHeaderOnly() {
        var output = tempDir.resolve("methods.csv");
        runner.run(new DefaultApplicationArguments(
                "--quiet", "--package=no.such.package",
                "--methods-output=" + output, fixturesDir.toString()));

        assertThat(Files.exists(output)).isTrue();
        var lines = linesOf(output);
        assertThat(lines).hasSize(1);
    }

    @Test
    void run_invalidFormat_fallsBackToCsv() {
        var output = tempDir.resolve("methods.out");
        runner.run(new DefaultApplicationArguments(
                "--quiet", "--package=cherry.classscanner.fixtures", "--format=xml",
                "--methods-output=" + output, fixturesDir.toString()));

        assertThat(contentOf(output)).contains("ソースパス,クラス名");
    }

    @Test
    void run_multipleInputs_aggregatesIntoSingleCsvWithHeaderOnce() {
        var output = tempDir.resolve("classes.csv");
        runner.run(new DefaultApplicationArguments(
                "--quiet", "--package=cherry.classscanner.fixtures",
                "--classes-output=" + output, fixturesDir.toString(), fixturesDir.toString()));

        var lines = linesOf(output);
        // ヘッダー1行 + (フィクスチャ2クラス分 x 2回のスキャン) = 5行
        assertThat(lines).hasSize(5);
    }

    @Test
    void run_ioErrorWritingOutput_exitCodeIsOne() {
        var output = tempDir.resolve("no-such-dir").resolve("methods.csv");
        runner.run(new DefaultApplicationArguments(
                "--quiet", "--package=cherry.classscanner.fixtures",
                "--methods-output=" + output, fixturesDir.toString()));

        assertThat(runner.getExitCode()).isOne();
    }

    private String contentOf(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<String> linesOf(Path path) {
        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

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

package cherry.classscanner.output;

import cherry.classscanner.model.ClassRecord;
import cherry.classscanner.model.MethodRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CSV/TSV出力(business-rules.md BR-4, BR-5, BR-6, BR-7)をExample-basedで検証する。
 * BR-8/BR-9改訂により、書き込みは常に新規(ヘッダーあり)の1回書き込みとなる(追記モードは廃止)。
 */
class CsvRecordWriterTest {

    private final CsvRecordWriter<MethodRecord> methodWriter = new CsvRecordWriter<>();
    private final CsvRecordWriter<ClassRecord> classWriter = new CsvRecordWriter<>();

    @TempDir
    Path tempDir;

    @Test
    void write_includesHeader() throws IOException {
        var file = tempDir.resolve("methods.csv");
        var record = sampleMethod();

        methodWriter.write(List.of(record), MethodRecord.class, "csv", file, StandardCharsets.UTF_8);

        var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertThat(lines.getFirst())
                .isEqualTo("ソースパス,クラス名,メソッド名,返却値,引数,修飾子,IsStatic,メソッドアノテーション,引数アノテーション");
        assertThat(lines).hasSize(2);
    }

    @Test
    void write_multipleRecords_allIncludedWithSingleHeader() throws IOException {
        var file = tempDir.resolve("methods.csv");

        methodWriter.write(List.of(sampleMethod(), sampleMethod()), MethodRecord.class, "csv", file,
                StandardCharsets.UTF_8);

        var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        // ヘッダー1行 + データ2行
        assertThat(lines).hasSize(3);
        assertThat(lines.get(1)).isEqualTo(lines.get(2));
    }

    @Test
    void write_joinsMultiValueFieldsWithDelimiter() throws IOException {
        var file = tempDir.resolve("methods.csv");
        var record = new MethodRecord(
                "src", "com.example.Foo", "bar", "void",
                List.of("java.lang.String", "int"),
                "public", false,
                List.of("com.example.A", "com.example.B"),
                List.of(List.of("com.example.P1", "com.example.P2"), List.of())
        );

        methodWriter.write(List.of(record), MethodRecord.class, "csv", file, StandardCharsets.UTF_8);

        var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        // 末尾に空白を含む値はCommons CSVによって自動的に引用符で囲まれる
        assertThat(lines.get(1)).isEqualTo(
                "src,com.example.Foo,bar,void,\"java.lang.String, int\",public,false," +
                        "\"com.example.A, com.example.B\",\"com.example.P1;com.example.P2 | \"");
    }

    @Test
    void write_tsvFormat_usesTabDelimiter() throws IOException {
        var file = tempDir.resolve("methods.tsv");

        methodWriter.write(List.of(sampleMethod()), MethodRecord.class, "tsv", file, StandardCharsets.UTF_8);

        var header = Files.readAllLines(file, StandardCharsets.UTF_8).getFirst();
        assertThat(header).contains("\t").doesNotContain(",");
    }

    @Test
    void write_emptyList_stillWritesHeaderOnly() throws IOException {
        var file = tempDir.resolve("empty.csv");

        methodWriter.write(List.of(), MethodRecord.class, "csv", file, StandardCharsets.UTF_8);

        var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertThat(lines).hasSize(1);
    }

    @Test
    void write_nullSuperclass_writesEmptyString() throws IOException {
        var file = tempDir.resolve("classes.csv");
        var record = new ClassRecord("src", "com.example.Foo", "Class", null,
                List.of(), "com.example", "public", List.of());

        classWriter.write(List.of(record), ClassRecord.class, "csv", file, StandardCharsets.UTF_8);

        var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertThat(lines.get(1)).isEqualTo("src,com.example.Foo,Class,,,com.example,public,");
    }

    private MethodRecord sampleMethod() {
        return new MethodRecord(
                "src", "com.example.Foo", "bar", "void",
                List.of(), "public", false, List.of(), List.of()
        );
    }
}

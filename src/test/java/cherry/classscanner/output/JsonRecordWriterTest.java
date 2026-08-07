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
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JSON出力(business-rules.md BR-4, BR-6, BR-7)をExample-basedで検証する。
 */
class JsonRecordWriterTest {

    private final JsonRecordWriter<MethodRecord> methodWriter = new JsonRecordWriter<>(JsonMapper.builder().build());
    private final JsonRecordWriter<ClassRecord> classWriter = new JsonRecordWriter<>(JsonMapper.builder().build());

    @TempDir
    Path tempDir;

    @Test
    void write_multiValueFieldsBecomeArrays() throws IOException {
        var file = tempDir.resolve("methods.json");
        var record = new MethodRecord(
                "src", "com.example.Foo", "bar", "void",
                List.of("java.lang.String", "int"), "public", false,
                List.of("com.example.A"), List.of(List.of("com.example.P1"), List.of())
        );

        methodWriter.write(List.of(record), MethodRecord.class, "json", file, StandardCharsets.UTF_8, false);

        var content = Files.readString(file, StandardCharsets.UTF_8);
        assertThat(content).contains("\"parameters\" : [ \"java.lang.String\", \"int\" ]");
        assertThat(content).contains("\"methodAnnotations\" : [ \"com.example.A\" ]");
        assertThat(content).contains("\"parameterAnnotations\" : [ [ \"com.example.P1\" ], [ ] ]");
    }

    @Test
    void write_nullSuperclass_isExplicitNull() throws IOException {
        var file = tempDir.resolve("classes.json");
        var record = new ClassRecord("src", "com.example.Foo", "Class", null,
                List.of(), "com.example", "public", List.of());

        classWriter.write(List.of(record), ClassRecord.class, "json", file, StandardCharsets.UTF_8, false);

        var content = Files.readString(file, StandardCharsets.UTF_8);
        assertThat(content).contains("\"superclass\" : null");
    }

    @Test
    void write_isPrettyPrinted() throws IOException {
        var file = tempDir.resolve("methods.json");
        methodWriter.write(List.of(sampleMethod()), MethodRecord.class, "json", file, StandardCharsets.UTF_8, false);

        var content = Files.readString(file, StandardCharsets.UTF_8);
        assertThat(content).contains("\n");
    }

    @Test
    void write_emptyList_producesEmptyArray() throws IOException {
        var file = tempDir.resolve("empty.json");
        methodWriter.write(List.of(), MethodRecord.class, "json", file, StandardCharsets.UTF_8, false);

        var content = Files.readString(file, StandardCharsets.UTF_8).strip();
        assertThat(content).isEqualTo("[ ]");
    }

    private MethodRecord sampleMethod() {
        return new MethodRecord(
                "src", "com.example.Foo", "bar", "void",
                List.of(), "public", false, List.of(), List.of()
        );
    }
}

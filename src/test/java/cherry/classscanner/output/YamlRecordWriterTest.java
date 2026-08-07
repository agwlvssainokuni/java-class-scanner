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
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * YAML出力(business-rules.md BR-4, BR-6, BR-7)をExample-basedで検証する。
 */
class YamlRecordWriterTest {

    private final YamlRecordWriter<MethodRecord> methodWriter = new YamlRecordWriter<>(YAMLMapper.builder().build());
    private final YamlRecordWriter<ClassRecord> classWriter = new YamlRecordWriter<>(YAMLMapper.builder().build());

    @TempDir
    Path tempDir;

    @Test
    void write_multiValueFieldsBecomeSequences() throws IOException {
        var file = tempDir.resolve("methods.yaml");
        var record = new MethodRecord(
                "src", "com.example.Foo", "bar", "void",
                List.of("java.lang.String", "int"), "public", false,
                List.of("com.example.A"), List.of()
        );

        methodWriter.write(List.of(record), MethodRecord.class, "yaml", file, StandardCharsets.UTF_8);

        var content = Files.readString(file, StandardCharsets.UTF_8);
        assertThat(content).contains("parameters:");
        assertThat(content).contains("- \"java.lang.String\"");
        assertThat(content).contains("- \"int\"");
    }

    @Test
    void write_nullSuperclass_isExplicitNull() throws IOException {
        var file = tempDir.resolve("classes.yaml");
        var record = new ClassRecord("src", "com.example.Foo", "Class", null,
                List.of(), "com.example", "public", List.of());

        classWriter.write(List.of(record), ClassRecord.class, "yaml", file, StandardCharsets.UTF_8);

        var content = Files.readString(file, StandardCharsets.UTF_8);
        assertThat(content).contains("superclass: null");
    }

    @Test
    void write_emptyList_producesEmptySequence() throws IOException {
        var file = tempDir.resolve("empty.yaml");
        methodWriter.write(List.of(), MethodRecord.class, "yaml", file, StandardCharsets.UTF_8);

        var content = Files.readString(file, StandardCharsets.UTF_8).strip();
        assertThat(content).isEqualTo("--- []");
    }
}

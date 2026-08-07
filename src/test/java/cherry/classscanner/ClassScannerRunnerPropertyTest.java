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
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.springframework.boot.DefaultApplicationArguments;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CSV出力の列構成に関する不変条件(先頭列は常にソースパス、全行のカラム数がヘッダーと一致する)を
 * jqwikによるProperty-Based Testで検証する(PBT-03)。
 */
class ClassScannerRunnerPropertyTest {

    @Property
    void csvOutput_everyRowHasSameColumnCountAsHeader_andFirstColumnIsSourcePath(
            @ForAll("packageFilters") String packageFilter,
            @ForAll("outputKinds") String outputKind
    ) {
        try {
            var fixturesDir = Path.of(SampleClass.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            var tempFile = Files.createTempFile("pbt-classscanner-", ".csv");
            try {
                var runner = new ClassScannerRunner(
                        new RecordExtractor(),
                        new CsvRecordWriter<>(),
                        new JsonRecordWriter<>(JsonMapper.builder().build()),
                        new YamlRecordWriter<>(YAMLMapper.builder().build())
                );

                runner.run(new DefaultApplicationArguments(
                        "--quiet", "--package=" + packageFilter,
                        "--" + outputKind + "-output=" + tempFile, fixturesDir.toString()));

                try (var reader = Files.newBufferedReader(tempFile, StandardCharsets.UTF_8);
                     var parser = CSVFormat.DEFAULT.builder().setHeader().get().parse(reader)) {
                    var headerSize = parser.getHeaderNames().size();
                    assertThat(parser.getHeaderNames().getFirst()).isEqualTo("ソースパス");
                    for (var record : parser) {
                        assertThat(record.size()).isEqualTo(headerSize);
                    }
                }
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Provide
    Arbitrary<String> packageFilters() {
        return Arbitraries.of(
                "cherry.classscanner.fixtures",
                "cherry.classscanner.fixtures.SampleClass",
                "cherry.classscanner",
                "no.such.package"
        );
    }

    @Provide
    Arbitrary<String> outputKinds() {
        return Arbitraries.of("classes", "methods", "fields", "constructors");
    }
}

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

package cherry.classscanner.extract;

import cherry.classscanner.model.ClassRecord;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * cherry.classscanner.fixturesパッケージを実際にClassGraphでスキャンし、RecordExtractorの
 * 抽出・フィルタ・ソートロジック(BR-1〜BR-3)をExample-basedで検証する。
 */
class RecordExtractorTest {

    private final RecordExtractor extractor = new RecordExtractor();
    private ScanResult scanResult;
    private List<ClassInfo> fixtureClasses;

    @BeforeEach
    void scanFixtures() {
        scanResult = new ClassGraph()
                .enableAllInfo()
                .acceptPackages("cherry.classscanner.fixtures")
                .scan();
        fixtureClasses = scanResult.getAllClasses().stream()
                .sorted(Comparator.comparing(ClassInfo::getName))
                .toList();
    }

    @AfterEach
    void closeScan() {
        scanResult.close();
    }

    @Test
    void filterAndSortByPackage_returnsAllClassesSortedByName_whenNoFilter() {
        var sorted = extractor.filterAndSortByPackage(scanResult.getAllClasses(), null);

        assertThat(sorted).extracting(ClassInfo::getName)
                .containsExactly(
                        "cherry.classscanner.fixtures.SampleClass",
                        "cherry.classscanner.fixtures.SampleInterface"
                );
    }

    @Test
    void filterAndSortByPackage_excludesNonMatchingPackage() {
        var filtered = extractor.filterAndSortByPackage(
                scanResult.getAllClasses(), List.of("no.such.package"));

        assertThat(filtered).isEmpty();
    }

    @Test
    void filterAndSortByPackage_trimsFilterValues() {
        var filtered = extractor.filterAndSortByPackage(
                scanResult.getAllClasses(), List.of("  cherry.classscanner.fixtures  "));

        assertThat(filtered).hasSize(2);
    }

    @Test
    void extractClasses_returnsExpectedFields() {
        var records = extractor.extractClasses("src", fixtureClasses);

        assertThat(records).hasSize(2);

        var sampleClass = records.stream()
                .filter(r -> r.className().equals("cherry.classscanner.fixtures.SampleClass"))
                .findFirst().orElseThrow();
        assertThat(sampleClass.sourcePath()).isEqualTo("src");
        assertThat(sampleClass.type()).isEqualTo("Class");
        assertThat(sampleClass.superclass()).isNull();
        assertThat(sampleClass.interfaces()).containsExactly("cherry.classscanner.fixtures.SampleInterface");
        assertThat(sampleClass.packageName()).isEqualTo("cherry.classscanner.fixtures");
        assertThat(sampleClass.modifiers()).isEqualTo("public");
        assertThat(sampleClass.classAnnotations()).isEmpty();

        var sampleInterface = records.stream()
                .filter(r -> r.className().equals("cherry.classscanner.fixtures.SampleInterface"))
                .findFirst().orElseThrow();
        assertThat(sampleInterface.type()).isEqualTo("Interface");
        assertThat(sampleInterface.modifiers()).isEqualTo("public abstract");
    }

    @Test
    void extractMethods_filtersOutLambdaAndConstructorButKeepsRegularMethods_sortedByName() {
        var records = extractor.extractMethods("src", fixtureClasses);

        // BR-1: <init>/<clinit>/lambda$*は除外。BR-2: メソッド名昇順ソート(クラスごと)。
        // クラス単位でソートするため、SampleClass分(4件)の後にSampleInterface分(1件)が続く。
        assertThat(records).extracting("methodName")
                .containsExactly(
                        "annotatedMethod",
                        "interfaceMethod",
                        "lambdaUsage",
                        "staticMethod",
                        "interfaceMethod" // SampleInterface自身のinterfaceMethod
                );
    }

    @Test
    void extractMethods_capturesAnnotationsAndParameterAnnotations() {
        var records = extractor.extractMethods("src", fixtureClasses);

        var annotatedMethod = records.stream()
                .filter(m -> m.className().equals("cherry.classscanner.fixtures.SampleClass")
                        && m.methodName().equals("annotatedMethod"))
                .findFirst().orElseThrow();

        assertThat(annotatedMethod.returnType()).isEqualTo("java.lang.String");
        assertThat(annotatedMethod.parameters()).containsExactly("java.lang.String");
        assertThat(annotatedMethod.methodAnnotations())
                .containsExactlyInAnyOrder("jakarta.annotation.Nonnull", "java.lang.Deprecated");
        assertThat(annotatedMethod.parameterAnnotations())
                .containsExactly(List.of("jakarta.annotation.Nonnull"));
    }

    @Test
    void extractMethods_capturesStaticModifierAndFlag() {
        var records = extractor.extractMethods("src", fixtureClasses);

        var staticMethod = records.stream()
                .filter(m -> m.className().equals("cherry.classscanner.fixtures.SampleClass")
                        && m.methodName().equals("staticMethod"))
                .findFirst().orElseThrow();

        assertThat(staticMethod.modifiers()).isEqualTo("public static");
        assertThat(staticMethod.isStatic()).isTrue();
    }

    @Test
    void extractFields_returnsAllFieldsSortedByName() {
        var records = extractor.extractFields("src", fixtureClasses);

        var sampleClassFields = records.stream()
                .filter(f -> f.className().equals("cherry.classscanner.fixtures.SampleClass"))
                .toList();

        assertThat(sampleClassFields).extracting("fieldName")
                .containsExactly("STATIC_FIELD", "instanceField");

        var staticField = sampleClassFields.get(0);
        assertThat(staticField.modifiers()).isEqualTo("public static final");
        assertThat(staticField.isStatic()).isTrue();
        assertThat(staticField.fieldAnnotations()).isEmpty();

        var instanceField = sampleClassFields.get(1);
        assertThat(instanceField.modifiers()).isEqualTo("private final");
        assertThat(instanceField.isStatic()).isFalse();
        assertThat(instanceField.fieldAnnotations()).containsExactly("jakarta.annotation.Nonnull");
    }

    @Test
    void extractConstructors_sortedByParameterCount() {
        var records = extractor.extractConstructors("src", fixtureClasses);

        var sampleClassConstructors = records.stream()
                .filter(c -> c.className().equals("cherry.classscanner.fixtures.SampleClass"))
                .toList();

        assertThat(sampleClassConstructors).hasSize(1);
        var constructor = sampleClassConstructors.getFirst();
        assertThat(constructor.parameters()).containsExactly("java.lang.String");
        assertThat(constructor.modifiers()).isEqualTo("public");
        assertThat(constructor.constructorAnnotations()).isEmpty();
        assertThat(constructor.parameterAnnotations()).containsExactly(List.of("jakarta.annotation.Nonnull"));
    }

    @Test
    void extractClasses_emptyInput_producesEmptyList() {
        assertThat(extractor.extractClasses("src", List.of())).isEmpty();
    }
}

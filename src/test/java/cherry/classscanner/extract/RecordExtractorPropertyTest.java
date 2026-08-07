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

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BR-3(パッケージフィルタは前方一致、複数指定はOR条件、前後空白はトリムする)の不変条件を
 * jqwikによるProperty-Based Testで検証する(PBT-03)。
 */
class RecordExtractorPropertyTest {

    private final RecordExtractor extractor = new RecordExtractor();

    @Property
    void filterAndSortByPackage_matchesPrefixRuleForArbitraryFilters(
            @ForAll("packageFilters") List<String> filters
    ) {
        try (ScanResult scanResult = new ClassGraph()
                .enableAllInfo()
                .acceptPackages("cherry.classscanner.fixtures")
                .scan()) {
            var allClasses = scanResult.getAllClasses();

            var actual = extractor.filterAndSortByPackage(allClasses, filters);

            var expectedNames = allClasses.stream()
                    .map(ClassInfo::getName)
                    .filter(name -> filters.stream().anyMatch(pkg -> name.startsWith(StringUtils.trim(pkg))))
                    .sorted()
                    .toList();

            assertThat(actual).extracting(ClassInfo::getName).containsExactlyElementsOf(expectedNames);
        }
    }

    @Provide
    Arbitrary<List<String>> packageFilters() {
        var matching = Arbitraries.of(
                "cherry.classscanner.fixtures",
                "cherry.classscanner.fixtures.SampleClass",
                "  cherry.classscanner.fixtures  ",
                "cherry.classscanner"
        );
        var nonMatching = Arbitraries.of(
                "no.such.package",
                "cherry.classscanner.fixtures.SampleClassX",
                "com.example"
        );
        return Arbitraries.oneOf(matching, nonMatching).list().ofMinSize(0).ofMaxSize(5);
    }
}

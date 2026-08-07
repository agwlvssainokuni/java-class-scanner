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

import cherry.classscanner.model.MethodRecord;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MethodRecordのJSON/YAMLシリアライズが往復変換(serialize -> deserialize)で元の値を復元できることを
 * jqwikのProperty-Based Testで検証する(PBT-02)。アプリ自体はデシリアライズを行わないが、Writerが
 * 生成する構造の正しさを裏付ける往復テストとして有用。
 */
class JacksonRoundTripPropertyTest {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();
    private static final YAMLMapper YAML_MAPPER = YAMLMapper.builder().build();

    @Property
    void jsonRoundTrip_preservesMethodRecord(@ForAll("methodRecords") MethodRecord original) {
        var json = JSON_MAPPER.writeValueAsString(original);
        var restored = JSON_MAPPER.readValue(json, MethodRecord.class);

        assertThat(restored).isEqualTo(original);
    }

    @Property
    void yamlRoundTrip_preservesMethodRecord(@ForAll("methodRecords") MethodRecord original) {
        var yaml = YAML_MAPPER.writeValueAsString(original);
        var restored = YAML_MAPPER.readValue(yaml, MethodRecord.class);

        assertThat(restored).isEqualTo(original);
    }

    @Property
    void jsonRoundTrip_preservesMethodRecordList(@ForAll("methodRecordLists") List<MethodRecord> original) {
        var json = JSON_MAPPER.writeValueAsString(original);
        List<MethodRecord> restored = JSON_MAPPER.readValue(json, JSON_MAPPER.getTypeFactory()
                .constructCollectionType(List.class, MethodRecord.class));

        assertThat(restored).isEqualTo(original);
    }

    @Provide
    Arbitrary<MethodRecord> methodRecords() {
        var identifiers = Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(10);
        var identifierLists = identifiers.list().ofMinSize(0).ofMaxSize(3);
        var nestedIdentifierLists = identifierLists.list().ofMinSize(0).ofMaxSize(3);
        // Combinators.combineは最大8引数までのため、先頭2項目(sourcePath, className)をTupleにまとめる
        var sourceAndClassName = Combinators.combine(identifiers, identifiers).as(Tuple::of);

        return Combinators.combine(
                sourceAndClassName, identifiers, identifiers,
                identifierLists, identifiers, Arbitraries.of(true, false),
                identifierLists, nestedIdentifierLists
        ).as((sourceAndClass, methodName, returnType, parameters, modifiers, isStatic,
              methodAnnotations, parameterAnnotations) -> new MethodRecord(
                sourceAndClass.get1(), sourceAndClass.get2(), methodName, returnType, parameters,
                modifiers, isStatic, methodAnnotations, parameterAnnotations
        ));
    }

    @Provide
    Arbitrary<List<MethodRecord>> methodRecordLists() {
        return methodRecords().list().ofMinSize(0).ofMaxSize(5);
    }
}

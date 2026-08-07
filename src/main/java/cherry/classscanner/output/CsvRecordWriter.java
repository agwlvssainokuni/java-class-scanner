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
import cherry.classscanner.model.ConstructorRecord;
import cherry.classscanner.model.FieldRecord;
import cherry.classscanner.model.MethodRecord;
import jakarta.annotation.Nonnull;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CSV/TSV用のRecordWriter実装。列構成・デリミタ結合(business-rules.md BR-4, BR-5, BR-6)を踏襲する。
 * 呼び出し側が全入力を集約したリストを1回だけ渡す想定のため、常にヘッダー付きで新規書き込みする
 * (BR-8/BR-9改訂)。
 */
@Component
public class CsvRecordWriter<T> implements RecordWriter<T> {

    @Override
    public void write(
            @Nonnull List<T> records,
            @Nonnull Class<T> type,
            @Nonnull String format,
            @Nonnull Path outputPath,
            @Nonnull Charset charset
    ) throws IOException {
        var baseFormat = "tsv".equals(format) ? CSVFormat.TDF : CSVFormat.DEFAULT;
        var csvFormat = baseFormat.builder().setHeader(headersFor(type).toArray(new String[0])).get();

        try (var writer = new FileWriter(outputPath.toFile(), charset, false);
             var printer = new CSVPrinter(writer, csvFormat)) {
            for (var record : records) {
                printer.printRecord(rowValues(record));
            }
        }
    }

    @Nonnull
    private List<String> headersFor(@Nonnull Class<T> type) {
        if (type == ClassRecord.class) {
            return List.of("ソースパス", "クラス名", "型", "親クラス", "実装インターフェース", "パッケージ", "修飾子", "クラスアノテーション");
        } else if (type == MethodRecord.class) {
            return List.of("ソースパス", "クラス名", "メソッド名", "返却値", "引数", "修飾子", "IsStatic", "メソッドアノテーション", "引数アノテーション");
        } else if (type == FieldRecord.class) {
            return List.of("ソースパス", "クラス名", "フィールド名", "フィールド型", "修飾子", "IsStatic", "フィールドアノテーション");
        } else if (type == ConstructorRecord.class) {
            return List.of("ソースパス", "クラス名", "引数", "修飾子", "コンストラクタアノテーション", "引数アノテーション");
        }
        throw new IllegalArgumentException("Unsupported record type: " + type);
    }

    @Nonnull
    private List<Object> rowValues(@Nonnull T record) {
        return switch (record) {
            case ClassRecord r -> List.of(
                    r.sourcePath(), r.className(), r.type(), StringUtils.defaultString(r.superclass()),
                    join(r.interfaces()), r.packageName(), r.modifiers(), join(r.classAnnotations())
            );
            case MethodRecord r -> List.of(
                    r.sourcePath(), r.className(), r.methodName(), r.returnType(), join(r.parameters()),
                    r.modifiers(), r.isStatic(), join(r.methodAnnotations()), joinNested(r.parameterAnnotations())
            );
            case FieldRecord r -> List.of(
                    r.sourcePath(), r.className(), r.fieldName(), r.fieldType(), r.modifiers(), r.isStatic(),
                    join(r.fieldAnnotations())
            );
            case ConstructorRecord r -> List.of(
                    r.sourcePath(), r.className(), join(r.parameters()), r.modifiers(),
                    join(r.constructorAnnotations()), joinNested(r.parameterAnnotations())
            );
            default -> throw new IllegalArgumentException("Unsupported record type: " + record.getClass());
        };
    }

    @Nonnull
    private String join(@Nonnull List<String> values) {
        return String.join(", ", values);
    }

    @Nonnull
    private String joinNested(@Nonnull List<List<String>> values) {
        return values.stream()
                .map(inner -> String.join(";", inner))
                .collect(Collectors.joining(" | "));
    }
}

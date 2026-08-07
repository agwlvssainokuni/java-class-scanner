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

import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

/**
 * DTO(model)のリストを指定フォーマットでファイルへ書き出す書式化層の共通インターフェース(Strategyパターン)。
 * {@code type}は、recordsが空でもCSV/TSV実装がヘッダーを解決できるようにするための型トークン(business-rules.md BR-7)。
 * {@code format}はCSV/TSVの区別に使う("csv"または"tsv")。JSON/YAML用実装は{@code type}/{@code format}を使用しない。
 * {@code append}はCSV/TSVの追記制御に使う(BR-8)。JSON/YAML用実装は全入力集約後の1回書き込みのみのため使用しない(BR-9)。
 */
public interface RecordWriter<T> {

    void write(
            @Nonnull List<T> records,
            @Nonnull Class<T> type,
            @Nonnull String format,
            @Nonnull Path outputPath,
            @Nonnull Charset charset,
            boolean append
    ) throws IOException;
}

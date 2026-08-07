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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

/**
 * DTO(model)のリストを指定フォーマットでファイルへ書き出す書式化層の共通インターフェース(Strategyパターン)。
 * 全フォーマット共通で、呼び出し側(ClassScannerRunner)が全入力を集約したリストを1回だけ渡す想定のため
 * 追記(append)の概念は持たない(business-rules.md BR-8/BR-9改訂)。
 * {@code type}は、recordsが空でもCSV/TSV実装がヘッダーを解決できるようにするための型トークン(BR-7)。
 * {@code format}はCSV/TSVの区別に使う("csv"または"tsv")。JSON/YAML用実装は{@code type}/{@code format}を使用しない。
 */
public interface RecordWriter<T> {

    void write(
            List<T> records,
            Class<T> type,
            String format,
            Path outputPath,
            Charset charset
    ) throws IOException;
}

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

import org.springframework.stereotype.Component;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * YAML用のRecordWriter実装。複数値項目はネイティブなシーケンス、null相当値は明示的なnullとして出力する
 * (business-rules.md BR-4, BR-6)。全入力を集約したリストを1回だけ書き込む想定のため追記は行わない
 * (BR-8/BR-9)。YAMLは元来インデント付きの人間可読形式であるため、JSONのような追加のpretty-print設定は不要。
 */
@Component
public class YamlRecordWriter<T> implements RecordWriter<T> {

    private final YAMLMapper yamlMapper;

    public YamlRecordWriter(YAMLMapper yamlMapper) {
        this.yamlMapper = yamlMapper;
    }

    @Override
    public void write(
            List<T> records,
            Class<T> type,
            String format,
            Path outputPath,
            Charset charset
    ) throws IOException {
        try (var writer = new OutputStreamWriter(Files.newOutputStream(outputPath), charset)) {
            yamlMapper.writeValue(writer, records);
        }
    }
}

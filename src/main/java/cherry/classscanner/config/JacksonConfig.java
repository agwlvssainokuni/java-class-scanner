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

package cherry.classscanner.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * JSON/YAML出力用のマッパーをSpring管理Beanとして提供する(nfr-design/logical-components.md参照)。
 * <p>
 * {@code YAMLMapper}は{@code ObjectMapper}のサブクラスであるため、Spring Bootの自動構成が提供する
 * 既定の{@code ObjectMapper}Beanに任せると、型による自動配線が{@code YAMLMapper}Beanと衝突し
 * 意図しない方が注入される場合がある。そのため、JSON用の{@code ObjectMapper}もここで明示的に定義し、
 * 呼び出し側では{@code @Qualifier}で確実に区別する。
 */
@Configuration
public class JacksonConfig {

    @Bean("jsonMapper")
    public ObjectMapper jsonMapper() {
        return JsonMapper.builder().build();
    }

    @Bean("yamlMapper")
    public YAMLMapper yamlMapper() {
        return YAMLMapper.builder().build();
    }
}

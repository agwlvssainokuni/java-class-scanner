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

package cherry.classscanner.model;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * superclassはインターフェースやObject自身のように親クラスを持たないクラスではnullとなる
 * (JSON/YAML出力では明示的なnullとして表現し、キー自体は省略しない。domain-entities.md参照)。
 */
public record ClassRecord(
        String sourcePath,
        String className,
        String type,
        @Nullable String superclass,
        List<String> interfaces,
        String packageName,
        String modifiers,
        List<String> classAnnotations
) {
}

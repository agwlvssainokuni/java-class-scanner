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

import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * parameterAnnotationsはparametersと同じ並び順・同じ要素数を持ち、
 * 各要素はその引数に付与されたアノテーション名のリスト(0件の場合は空リスト)。
 */
public record MethodRecord(
        @Nonnull String sourcePath,
        @Nonnull String className,
        @Nonnull String methodName,
        @Nonnull String returnType,
        @Nonnull List<String> parameters,
        @Nonnull String modifiers,
        boolean isStatic,
        @Nonnull List<String> methodAnnotations,
        @Nonnull List<List<String>> parameterAnnotations
) {
}

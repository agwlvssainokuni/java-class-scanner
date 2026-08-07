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
import cherry.classscanner.model.ConstructorRecord;
import cherry.classscanner.model.FieldRecord;
import cherry.classscanner.model.MethodRecord;
import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.MethodParameterInfo;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * ClassGraphのスキャン結果をドメインエンティティ(model)へ変換する抽出層。
 * ソート順・フィルタリングのビジネスルールはfunctional-design/business-rules.md BR-1〜BR-3を踏襲する。
 */
@Component
public class RecordExtractor {

    @Nonnull
    public List<ClassInfo> filterAndSortByPackage(
            @Nonnull List<ClassInfo> allClasses,
            @Nullable List<String> packageFilter
    ) {
        return allClasses.stream()
                .filter(classInfo -> matchesPackageFilter(classInfo.getName(), packageFilter))
                .sorted(Comparator.comparing(ClassInfo::getName))
                .toList();
    }

    @Nonnull
    public List<ClassRecord> extractClasses(
            @Nonnull String sourcePath,
            @Nonnull List<ClassInfo> classes
    ) {
        return classes.stream()
                .map(classInfo -> new ClassRecord(
                        sourcePath,
                        classInfo.getName(),
                        classType(classInfo),
                        classInfo.getSuperclass() != null ? classInfo.getSuperclass().getName() : null,
                        classInfo.getInterfaces().stream().map(ClassInfo::getName).toList(),
                        classInfo.getPackageName(),
                        classInfo.getModifiersStr(),
                        annotationNames(classInfo.getAnnotationInfo())
                ))
                .toList();
    }

    @Nonnull
    public List<MethodRecord> extractMethods(
            @Nonnull String sourcePath,
            @Nonnull List<ClassInfo> classes
    ) {
        return classes.stream()
                .flatMap(classInfo -> classInfo.getMethodInfo().stream()
                        .filter(this::isRegularMethod)
                        .sorted(Comparator.comparing(MethodInfo::getName))
                        .map(methodInfo -> new MethodRecord(
                                sourcePath,
                                classInfo.getName(),
                                methodInfo.getName(),
                                methodInfo.getTypeSignatureOrTypeDescriptor().getResultType().toString(),
                                parameterTypes(methodInfo.getParameterInfo()),
                                methodInfo.getModifiersStr(),
                                methodInfo.isStatic(),
                                annotationNames(methodInfo.getAnnotationInfo()),
                                parameterAnnotationNames(methodInfo.getParameterInfo())
                        )))
                .toList();
    }

    @Nonnull
    public List<FieldRecord> extractFields(
            @Nonnull String sourcePath,
            @Nonnull List<ClassInfo> classes
    ) {
        return classes.stream()
                .flatMap(classInfo -> classInfo.getFieldInfo().stream()
                        .sorted(Comparator.comparing(io.github.classgraph.FieldInfo::getName))
                        .map(fieldInfo -> new FieldRecord(
                                sourcePath,
                                classInfo.getName(),
                                fieldInfo.getName(),
                                fieldInfo.getTypeSignatureOrTypeDescriptor().toString(),
                                fieldInfo.getModifiersStr(),
                                fieldInfo.isStatic(),
                                annotationNames(fieldInfo.getAnnotationInfo())
                        )))
                .toList();
    }

    @Nonnull
    public List<ConstructorRecord> extractConstructors(
            @Nonnull String sourcePath,
            @Nonnull List<ClassInfo> classes
    ) {
        return classes.stream()
                .flatMap(classInfo -> classInfo.getConstructorInfo().stream()
                        .sorted(Comparator.comparingInt(constructorInfo -> constructorInfo.getParameterInfo().length))
                        .map(constructorInfo -> new ConstructorRecord(
                                sourcePath,
                                classInfo.getName(),
                                parameterTypes(constructorInfo.getParameterInfo()),
                                constructorInfo.getModifiersStr(),
                                annotationNames(constructorInfo.getAnnotationInfo()),
                                parameterAnnotationNames(constructorInfo.getParameterInfo())
                        )))
                .toList();
    }

    @Nonnull
    private String classType(@Nonnull ClassInfo classInfo) {
        if (classInfo.isInterface()) {
            return "Interface";
        } else if (classInfo.isAbstract()) {
            return "Abstract Class";
        } else if (classInfo.isEnum()) {
            return "Enum";
        } else if (classInfo.isAnnotation()) {
            return "Annotation";
        } else {
            return "Class";
        }
    }

    private boolean matchesPackageFilter(
            @Nonnull String className,
            @Nullable List<String> packageFilter
    ) {
        if (packageFilter == null) {
            return true;
        }
        for (var pkg : packageFilter) {
            pkg = StringUtils.trim(pkg);
            if (className.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }

    private boolean isRegularMethod(@Nonnull MethodInfo methodInfo) {
        return !methodInfo.getName().equals("<init>") &&
                !methodInfo.getName().equals("<clinit>") &&
                !methodInfo.getName().contains("lambda$");
    }

    @Nonnull
    private List<String> parameterTypes(@Nonnull MethodParameterInfo[] parameters) {
        return Stream.of(parameters)
                .map(MethodParameterInfo::getTypeSignatureOrTypeDescriptor)
                .map(Object::toString)
                .toList();
    }

    @Nonnull
    private List<String> annotationNames(@Nonnull List<AnnotationInfo> annotations) {
        return annotations.stream()
                .map(AnnotationInfo::getName)
                .toList();
    }

    @Nonnull
    private List<List<String>> parameterAnnotationNames(@Nonnull MethodParameterInfo[] parameters) {
        return Stream.of(parameters)
                .map(param -> annotationNames(param.getAnnotationInfo()))
                .toList();
    }
}

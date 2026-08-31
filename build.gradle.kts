plugins {
    `java-library`
    id("io.spring.dependency-management") version "1.1.7"
    id("org.springframework.boot") version "4.1.1"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:4.1.1")
    }
    dependencies {
        dependency("io.github.classgraph:classgraph:4.8.194")
        dependency("org.apache.commons:commons-csv:1.14.1")
    }
}

dependencies {
    implementation("io.github.classgraph:classgraph")
    implementation("org.apache.commons:commons-csv")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.jspecify:jspecify")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("tools.jackson.core:jackson-databind")
    implementation("tools.jackson.dataformat:jackson-dataformat-yaml")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("net.jqwik:jqwik:1.10.1")
    // テストフィクスチャでClassGraphによる実行時アノテーション検出を検証するためだけに使用(null安全性とは無関係)
    testImplementation("jakarta.annotation:jakarta.annotation-api")
}

# Tech Stack Decisions — java-class-scanner

## 新規追加する依存関係

| 依存 | groupId:artifactId | バージョン | スコープ | 用途 |
|---|---|---|---|---|
| Jackson Databind | `tools.jackson.core:jackson-databind` | Spring Boot 4.1.0 BOM管理（明示指定なし、`jackson-bom.version`=3.1.4相当） | implementation | JSON出力（FR-2） |
| Jackson YAML | `tools.jackson.dataformat:jackson-dataformat-yaml` | Spring Boot 4.1.0 BOM管理（同上） | implementation | YAML出力（FR-2） |
| jqwik | `net.jqwik:jqwik` | `1.10.1`（Maven Central最新版、明示指定） | testImplementation | Property-Based Testing（Partial適用、PBT-02/03/07/08/09） |

## 選定理由

- **Jackson 3系（`tools.jackson`）を採用**: Spring Boot 4.1.0のBOMは、Jackson 2系（`com.fasterxml.jackson`, `jackson-2-bom.version`）とJackson 3系（`tools.jackson`, `jackson-bom.version`）の両方をインポートしているが、無印の`jackson-bom.version`（3.1.4）がSpring Boot 4.1の主系統として管理されている。本プロジェクトはJava 25・Spring Boot 4.1.0・Gradle 9.6.1と一貫して最新版を採用する方針であるため、Jackson 3系を採用する。
- **バージョンをBOM管理に委ねる理由**: 既存の`spring-boot-starter`等と同様の方針で、Spring Boot側のバージョン管理と将来のSpring Bootバージョンアップ時の追従性を優先する。
- **jqwikのバージョン明示指定**: Spring Boot BOMの管理対象外のため、Maven Central上の最新リリース版を明示的に指定する。

## 既存依存関係（変更なし）

| 依存 | 用途 |
|---|---|
| `spring-boot-starter` | DI・アプリケーションライフサイクル・`ApplicationRunner` |
| `spring-boot-starter-test`（JUnit 5, AssertJ含む） | テスト基盤 |
| `io.github.classgraph:classgraph:4.8.184` | クラス静的解析 |
| `org.apache.commons:commons-csv:1.14.1` | CSV/TSV出力 |
| `org.apache.commons:commons-lang3` | 文字列ユーティリティ |
| `jakarta.annotation:jakarta.annotation-api` | Null安全性アノテーション |

## build.gradleへの反映方針（Code Generation段階で実施）
```groovy
dependencies {
    // 既存の依存に加えて
    implementation 'tools.jackson.core:jackson-databind'
    implementation 'tools.jackson.dataformat:jackson-dataformat-yaml'
    testImplementation 'net.jqwik:jqwik:1.10.1'
}
```
具体的な反映はNFR Design/Code Generation段階で実施する。

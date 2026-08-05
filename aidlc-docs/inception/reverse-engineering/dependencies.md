# Dependencies

## Internal Dependencies

```mermaid
flowchart LR
    Main["Main"] -->|"Spring DIが起動時に検出・実行"| Runner["ClassScannerRunner"]
```

### Main は ClassScannerRunner に依存する
- **Type**: Runtime（間接依存。Spring DIコンテナが `@Component` として検出し、起動完了時に `ApplicationRunner#run()` を呼び出す。`Main` から `ClassScannerRunner` への直接のコンパイル時参照はない）
- **Reason**: `Main` はアプリケーション起動のみを担当し、実際のCLIロジックはSpring Bootの `ApplicationRunner` 機構経由で `ClassScannerRunner` に委譲される。

単一パッケージ・単一モジュール構成のため、パッケージ間依存は上記のみ。

## External Dependencies

### org.springframework.boot:spring-boot-starter
- **Version**: 4.1.0（`spring-boot-dependencies` BOM経由）
- **Purpose**: DIコンテナ、`ApplicationRunner`/`ExitCodeGenerator`/`ApplicationArguments`、ロギング基盤（SLF4J）の提供。
- **License**: Apache License 2.0

### org.springframework.boot:spring-boot-starter-test
- **Version**: 4.1.0（`spring-boot-dependencies` BOM経由）
- **Purpose**: テスト基盤（JUnit 5等）。現状テストコードは未実装だが依存は準備済み。
- **License**: Apache License 2.0

### io.github.classgraph:classgraph
- **Version**: 4.8.184
- **Purpose**: JAR/クラスファイルの静的解析によるクラス構造抽出。本ツールの中核機能。
- **License**: MIT License

### org.apache.commons:commons-csv
- **Version**: 1.14.1
- **Purpose**: CSV/TSVファイルの書き込み（`CSVPrinter`/`CSVFormat`）。
- **License**: Apache License 2.0

### org.apache.commons:commons-lang3
- **Version**: Spring Boot BOM解決バージョン（`build.gradle` にバージョン明示なし）
- **Purpose**: `StringUtils.trim()` によるパッケージフィルタ文字列の整形。
- **License**: Apache License 2.0

### jakarta.annotation:jakarta.annotation-api
- **Version**: Spring Boot BOM解決バージョン
- **Purpose**: `@Nonnull`/`@Nullable` アノテーションによるnull安全性表明。
- **License**: EPL 2.0 / GPL2 w/ CPE（デュアルライセンス）

## ビルド時専用依存（Gradleプラグイン）

### org.springframework.boot（Gradleプラグイン）
- **Version**: 4.1.0
- **Purpose**: `bootJar`タスクによる実行可能JAR生成、Spring Boot BOM連携。

### io.spring.dependency-management（Gradleプラグイン）
- **Version**: 1.1.7
- **Purpose**: Maven BOM（`spring-boot-dependencies`）のインポートによる依存バージョンの一元管理。

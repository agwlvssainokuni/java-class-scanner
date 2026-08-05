# Technology Stack

## Programming Languages

- Java - 25 (Temurin) - アプリケーション全体の実装言語（ソース/ターゲット互換性ともに25）

## Frameworks

- Spring Boot - 4.1.0 - DIコンテナ、アプリケーションライフサイクル管理、コマンドライン引数パース（`ApplicationRunner`/`ApplicationArguments`）
- ClassGraph - 4.8.184 - JAR/クラスファイルの静的解析（クラス・メソッド・フィールド・コンストラクタ・アノテーション抽出）

## Infrastructure

なし（クラウド/インフラサービスへの依存なし。ローカル実行のスタンドアロンCLI）

## Build Tools

- Gradle - 9.6.1（Gradle Wrapper経由） - ビルド・依存管理・実行可能JAR生成（`bootJar`）
- Spring Boot Gradle Plugin - 4.1.0 - `bootJar`タスク、依存BOM連携
- io.spring.dependency-management プラグイン - 1.1.7 - Spring Boot BOMのインポートとバージョン管理

## Testing Tools

- JUnit 5 Platform - `build.gradle` の `Test` タスクで `useJUnitPlatform()` を設定済み、`spring-boot-starter-test` を `testImplementation` で依存追加済み
- **現状**: テストコード自体は未実装（`src/test` ディレクトリなし）。基盤のみ整備されている状態。

## その他ライブラリ

- Apache Commons CSV - 1.14.1 - CSV/TSV出力
- Apache Commons Lang3 - Spring Boot BOM解決バージョン - 文字列ユーティリティ（`StringUtils.trim`）
- jakarta.annotation-api - Spring Boot BOM解決バージョン - `@Nonnull`/`@Nullable` アノテーション
- SLF4J（Spring Boot同梱） - ロギングファサード

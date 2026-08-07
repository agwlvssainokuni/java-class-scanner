# Build Instructions

## Prerequisites
- **Build Tool**: Gradle 9.6.1（同梱のGradle Wrapperを使用、別途インストール不要）
- **JDK**: Java 25（Temurin等）。`build.gradle`のtoolchainで固定
- **依存関係**: `build.gradle`に記載の全依存はSpring Boot 4.1.0のBOM経由、またはバージョン明示で解決される（Maven Centralへのネットワークアクセスが必要）
- **環境変数**: 不要
- **システム要件**: 特別な要件なし（ローカルCLIツールのビルド・実行に必要な標準的なディスク容量・メモリで十分）

## Build Steps

### 1. 依存関係のインストール
```bash
./gradlew dependencies
```
（省略可。`build`/`test`実行時に自動解決される）

### 2. 環境設定
不要。追加の環境変数や認証情報の設定は必要ない。

### 3. 全体ビルド
```bash
./gradlew clean build
```
このコマンドはコンパイル・テスト実行・実行可能JAR生成（`bootJar`）を一括で行う。

### 4. ビルド成功の確認
- **期待される出力**: `BUILD SUCCESSFUL` で終了し、`> Task :test`・`> Task :build`が成功していること
- **生成される成果物**:
  - `build/libs/java-class-scanner.jar` — 実行可能JAR（`java -jar`で直接実行可能）
  - `build/libs/java-class-scanner-plain.jar` — 依存を含まない素のJAR（Spring Boot標準の副産物、通常は使用しない）
  - `build/classes/java/main/` — コンパイル済みクラス（デモ実行時のスキャン対象にも利用可能）
  - `build/test-results/test/` — JUnit Platform形式のテスト結果XML
  - `build/reports/tests/test/` — HTML形式のテストレポート
- **許容される警告**: `Consider enabling configuration cache to speed up this build` というGradleの提案メッセージは無視してよい（機能・正当性に影響しない）

## Troubleshooting

### ビルドが依存関係エラーで失敗する
- **原因**: Maven Centralへのネットワークアクセスがブロックされている、またはSpring Boot BOM（`spring-boot-dependencies:4.1.0`）が解決できない
- **解決策**: ネットワーク接続を確認し、`./gradlew build --refresh-dependencies`で依存キャッシュを再取得する

### ビルドがコンパイルエラーで失敗する
- **原因**: JDK 25以外のJavaが使われている、またはtoolchainが正しく解決されていない
- **解決策**: `java -version`でJDK 25系であることを確認する。Gradle toolchain機能によりJDK 25が自動検出・使用されるが、見つからない場合は`./gradlew -Porg.gradle.java.installations.auto-download=true build`のように自動ダウンロードを許可する、またはJDK 25を手動インストールしてPATH/JAVA_HOMEを設定する

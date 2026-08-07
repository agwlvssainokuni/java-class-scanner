# Integration Test Instructions

## 目的
`RecordExtractor`・`CsvRecordWriter`/`JsonRecordWriter`/`YamlRecordWriter`・`ClassScannerRunner`がSpring DIで実際に組み立てられた状態で正しく連携動作することを確認する。ユニットテストは各コンポーネントを個別に（または`ClassScannerRunnerTest`ではDIを介さず手動組み立てで）検証しているのに対し、本統合テストは**実際にビルドした実行可能JARを、本アプリとは無関係な外部のJavaプロジェクトに対して実行**し、エンドツーエンドの挙動を確認する点が異なる。

本プロジェクトは単一プロセスのCLIツールであり、複数サービス間の連携は存在しないため、「サービス間統合テスト」ではなく「アプリケーション全体としての結合動作確認」を統合テストと位置づける。

## テストシナリオ

### シナリオ1: 実行可能JAR起動によるSpring DI組み立ての確認
- **説明**: `java -jar`起動時にSpring Bootが`RecordExtractor`・各`RecordWriter`実装・`ClassScannerRunner`を正しくDI解決できること（NFR Design Q1=Bで確定したDIベース構成の動作確認、および過去に発見した`ObjectMapper`/`YAMLMapper`型衝突バグの再発がないこと）
- **セットアップ**: `./gradlew bootJar`
- **手順**: `java -jar build/libs/java-class-scanner.jar`を引数なしで実行
- **期待結果**: アプリケーションが起動時エラー（`BeanCreationException`等）なくusageメッセージを表示して終了する

### シナリオ2: 外部プロジェクトに対するスキャン〜出力のエンドツーエンド確認
- **説明**: 自身とは無関係な小規模Javaプロジェクト（`demo/src/com/example/library`）を対象に、主要なCLIオプションの組み合わせを一通り実行し、出力ファイルの内容を目視確認する
- **セットアップ**: `demo/src`配下のサンプルソースをjavacでその場コンパイル（`demo.sh`が自動実行）
- **テスト手順**: `./demo.sh`を実行（`./gradlew bootJar`が事前に必要）
- **確認するポイント**（`demo.sh`内の11セクションに対応）:
  1. コンソールへの標準クラス一覧表示
  2. `--verbose`による詳細表示（修飾子・アノテーション含む）
  3. `--classes-output`によるCSV出力
  4. `--format=json --methods-output`によるJSON出力（配列表現）
  5. `--format=yaml --fields-output`によるYAML出力（配列表現、静的フィールド含む）
  6. `--format=tsv --constructors-output`によるTSV出力
  7. ディレクトリとJARを同時指定した複数入力集約（`sourcePath`がディレクトリ/JARそれぞれで異なることを確認）
  8. 不正な`--format`値のCSVへのフォールバック
  9. 不正な`--charset`値のUTF-8へのフォールバック
  10. 該当データ0件時の空配列/ヘッダーのみ出力
  11. 引数なし実行時のusage表示
- **期待結果**: 全セクションが例外なく完走し、各出力ファイルの内容が該当BR（BR-4〜BR-13）と一致する
- **クリーンアップ**: `demo.sh`終了時に一時出力ファイル（`mktemp -d`で作成）は自動削除される。`demo/build/`（コンパイル成果物）は次回実行時に再作成されるため放置してよい（`.gitignore`で除外済み）

## 統合テスト環境のセットアップ

### 1. 実行可能JARのビルド
```bash
./gradlew bootJar
```

### 2. 追加の環境設定
不要（外部サービス・データベース等への依存がないため）。

## 統合テストの実行

### 1. 統合テストスイートの実行
```bash
./demo.sh
```

### 2. 連携動作の確認
- **確認するシナリオ**: 上記シナリオ1・2
- **期待結果**: 標準出力に全11セクションのヘッダーと結果が表示され、エラーなく完走する（終了コード0）
- **ログの場所**: 標準出力に直接表示される（別途ログファイルには出力しない）

### 3. クリーンアップ
```bash
rm -rf demo/build
```
（`demo.sh`再実行時に自動的に再作成されるため、通常は明示的なクリーンアップは不要）

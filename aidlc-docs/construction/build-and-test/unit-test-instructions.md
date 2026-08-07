# Unit Test Execution

## テスト構成
- **フレームワーク**: JUnit 5 Platform + AssertJ（Example-basedテスト）、jqwik（Property-Based Testing）
- **対象**: `src/test/java/cherry/classscanner/` 配下の8テストクラス、計38テストケース
  - `extract/RecordExtractorTest.java`（Example-based, 10件）
  - `extract/RecordExtractorPropertyTest.java`（jqwik PBT-03, 1プロパティ）
  - `output/CsvRecordWriterTest.java`（Example-based, 6件）
  - `output/JsonRecordWriterTest.java`（Example-based, 4件）
  - `output/YamlRecordWriterTest.java`（Example-based, 3件）
  - `output/JacksonRoundTripPropertyTest.java`（jqwik PBT-02, 3プロパティ）
  - `ClassScannerRunnerTest.java`（Example-based, 10件）
  - `ClassScannerRunnerPropertyTest.java`（jqwik PBT-03, 1プロパティ）
- ClassGraphの`ClassInfo`/`MethodInfo`等は合成が困難なため、モックではなく`src/test/java/cherry/classscanner/fixtures/`配下の実フィクスチャクラス（`SampleClass`, `SampleInterface`）を実際にスキャンして検証する

## テストの実行

### 1. 全ユニットテストの実行
```bash
./gradlew test
```

### 2. テスト結果の確認
- **期待値**: 38テスト全て成功、失敗0件
- **カバレッジ**: カバレッジ計測ツール（JaCoCo等）は導入していないため、数値目標は設定していない。抽出層(`RecordExtractor`)・書式化層(`CsvRecordWriter`/`JsonRecordWriter`/`YamlRecordWriter`)・オーケストレーション層(`ClassScannerRunner`)それぞれに専用テストクラスがあり、主要な分岐（BR-1〜BR-13）を網羅している
- **テストレポートの場所**:
  - XML: `build/test-results/test/TEST-*.xml`
  - HTML: `build/reports/tests/test/index.html`

### 3. jqwikのProperty-Based Testについて
jqwikの各`@Property`メソッドは既定で1000ケースをランダム生成して検証する（`--tests`フィルタで個別実行時に`-i`オプションを付けると`tries = 1000`等の詳細ログが確認できる）。失敗時はjqwikが失敗ケースを自動的に最小化(shrinking)し、再現用のseed値をログに出力する。

**注意**: jqwikのコンソールバナーにAIエージェント宛てを装った不審な文言（プロンプトインジェクションの疑いがある指示文）が含まれることを確認済み。実行結果自体は正規のものであり、テスト実行・レポート内容の信頼性には影響しない。

### 4. テスト失敗時の対応
テストが失敗した場合:
1. `build/reports/tests/test/index.html`または`build/test-results/test/`のXMLでどのテストが失敗したか確認する
2. 失敗したテストクラスのassertionメッセージを確認する
3. jqwikのプロパティテストが失敗した場合は、出力される`seed`値を使い`@Property(seed = "...")`で失敗ケースを再現する
4. コード側の問題であれば修正し、テスト側の期待値誤りであればテストを修正する
5. `./gradlew test`を再実行し、全テストが成功することを確認する

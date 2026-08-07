# Code Generation Plan — java-class-scanner

## Unit Context
- **Unit**: java-class-scanner（単一ユニット、Units Generation省略）
- **Workspace Root**: `~/Documents/project/git/java-class-scanner`
- **プロジェクト種別**: Brownfield（既存ファイルは原則インプレース修正。新規ファイルのみ新設）
- **参照する設計成果物**:
  - `aidlc-docs/inception/requirements/requirements.md`（FR-1〜FR-8, NFR-1〜NFR-4）
  - `aidlc-docs/inception/application-design/`（コンポーネント構成）
  - `aidlc-docs/construction/java-class-scanner/functional-design/`（ドメインモデル・ビジネスルールBR-1〜BR-13）
  - `aidlc-docs/construction/java-class-scanner/nfr-requirements/tech-stack-decisions.md`（依存バージョン）
  - `aidlc-docs/construction/java-class-scanner/nfr-design/logical-components.md`（DIベースのコンポーネント管理）

## 実装レベルの補足決定（本計画で確定、Q&A不要な実装詳細）
`RecordWriter<T>`インターフェースの共通シグネチャを以下の通り確定した（Application Design Q4「共通インターフェース+Strategyパターン」を実装可能な形に具体化）:
```java
public interface RecordWriter<T> {
    void write(List<T> records, Class<T> type, String format, Path outputPath, Charset charset, boolean append) throws IOException;
}
```
`type`（`Class<T>`）を渡すことで、`CsvRecordWriter`はレコード0件（BR-7）でも対象の型に応じたヘッダーを解決できる。`format`はCSV/TSVの区別に使う（`CsvRecordWriter`が単一実装でCSV/TSV両方を扱うため、計画時点の設計から追加）。CSV/TSVはJava 25のパターンマッチング`switch`で型ごとの行変換ロジックを持つ。JSON/YAML用Writerは`type`/`format`引数を使用しない（Jacksonが対象インスタンスから直接シリアライズするため）。

**実装中に発見・修正した設計上の問題**: `tools.jackson.dataformat.yaml.YAMLMapper`は`tools.jackson.databind.ObjectMapper`のサブクラスであるため、Spring Bootの自動構成が提供する既定`ObjectMapper`Beanに型ベースの自動配線を任せると、`YAMLMapper`Beanとの型衝突により意図しない方が注入される不具合を実機確認した（`--format=json`指定時にYAML形式で出力される事象）。対策として`JacksonConfig`でJSON用`ObjectMapper`も明示的な`@Bean`として定義し、`JsonRecordWriter`のコンストラクタで`@Qualifier`により確実に区別するよう修正した（NFR Designの「Spring Boot自動構成の既定Beanを利用」からの変更）。Bean名は`jsonMapper`/`yamlMapper`で命名を統一する。

## Steps

- [x] **Step 1: build.gradle 依存関係更新**
  - 対象: `build.gradle`（修正）
  - 内容: `tools.jackson.core:jackson-databind`, `tools.jackson.dataformat:jackson-dataformat-yaml` を`implementation`へ追加（バージョン明示なし、Spring Boot BOM管理）。`net.jqwik:jqwik:1.10.1` を`testImplementation`へ追加。

- [x] **Step 2: ドメインモデル生成**
  - 対象（新規）: `src/main/java/cherry/classscanner/model/ClassRecord.java`, `MethodRecord.java`, `FieldRecord.java`, `ConstructorRecord.java`
  - 内容: `domain-entities.md`のフィールド定義に基づきJava 25 recordとして実装。

- [x] **Step 3: 抽出層生成**
  - 対象（新規）: `src/main/java/cherry/classscanner/extract/RecordExtractor.java`
  - 内容: `@Component`。既存`ClassScannerRunner`の抽出・フィルタ・ソートロジック（BR-1〜BR-3、パッケージフィルタ含む）を移設し、`extractClasses`/`extractMethods`/`extractFields`/`extractConstructors`の4メソッドとして実装。

- [x] **Step 4: 出力層生成**
  - 対象（新規）: `src/main/java/cherry/classscanner/output/RecordWriter.java`（interface）, `CsvRecordWriter.java`, `JsonRecordWriter.java`, `YamlRecordWriter.java`
  - 内容: `@Component`。CSV/TSVは既存の`CSVPrinter`ベースの出力・ヘッダー追記制御（BR-8）・複数値項目のデリミタ結合（BR-4/BR-5）を移設。JSON/YAMLはJackson（コンストラクタインジェクションされた`ObjectMapper`/`YAMLMapper`）でシリアライズし、配列表現（BR-4）・null明示（BR-6）・pretty-print（JSONのみ）を実装。

- [x] **Step 5: Jackson設定生成**
  - 対象（新規）: `src/main/java/cherry/classscanner/config/JacksonConfig.java`
  - 内容: `@Configuration`。`YAMLMapper`の`@Bean`定義。JSON用`ObjectMapper`の`@Bean`定義も追加（実装中に発見した設計上の問題への対処、上記補足決定を参照）。

- [x] **Step 6: オーケストレーション層改修**
  - 対象（修正）: `src/main/java/cherry/classscanner/ClassScannerRunner.java`
  - 内容:
    - `RecordExtractor`・3種の`RecordWriter`をコンストラクタインジェクション
    - CLIオプションのリネーム・追加: `--methods-csv`→`--methods-output`, `--fields-csv`→`--fields-output`, `--constructors-csv`→`--constructors-output`（旧名は削除、後方互換なし）, 新規`--classes-output`
    - `--format`を`csv`/`tsv`/`json`/`yaml`の4値対応に拡張（不正値はCSVへフォールバック、BR-11）
    - CSV/TSVは逐次書き込み、JSON/YAMLは集約後の1回書き込み（0件でも空配列を出力、BR-7/BR-9）を実装
    - JSON/YAML書き込み中のエラー時ベストエフォート出力（BR-12）を実装
    - `--verbose`コンソール出力に修飾子・クラスアノテーションを追加（BR-13）
    - 使用方法（usage）メッセージを新オプション体系に更新
    - 旧来の抽出・整形専用private メソッド（`outputMethodsToCsv`等、`parametersToString`等、`isRegularMethod`、`matchesPackageFilter`）は`RecordExtractor`/`RecordWriter`実装へ移設済みのため削除

- [ ] **Step 7: ドメインモデル・抽出層・出力層のテスト生成**
  - 対象（新規）:
    - `src/test/java/cherry/classscanner/extract/RecordExtractorTest.java`（Example-based）
    - `src/test/java/cherry/classscanner/extract/RecordExtractorPropertyTest.java`（jqwik PBT: BR-2ソート順不変条件、BR-3パッケージフィルタ不変条件 — PBT-03）
    - `src/test/java/cherry/classscanner/output/CsvRecordWriterTest.java`（Example-based: ヘッダー/追記制御、デリミタ結合）
    - `src/test/java/cherry/classscanner/output/JsonRecordWriterTest.java`（Example-based: 配列表現、null明示、pretty-print、jqwik round-trip PBT — PBT-02）
    - `src/test/java/cherry/classscanner/output/YamlRecordWriterTest.java`（Example-based + round-trip PBT — PBT-02）

- [ ] **Step 8: オーケストレーション層のテスト生成**
  - 対象（新規）:
    - `src/test/java/cherry/classscanner/ClassScannerRunnerTest.java`（Example-based。FR-7の既存機能バックフィル: 引数なし/quiet、終了コード、package フィルタ、charset/format不正値フォールバック、verbose出力。加えて新機能: classes-output、json/yaml出力、複数入力集約、旧オプション名廃止の確認）
    - `src/test/java/cherry/classscanner/ClassScannerRunnerPropertyTest.java`（jqwik PBT: CSV列構成不変条件「先頭列は常にソースパス」等 — PBT-03）

- [ ] **Step 9: ドキュメント更新**
  - 対象（修正）: `README.md`, `README_en.md`, `CLAUDE.local.md`
  - 内容: 新CLIオプション体系（`--classes-output`等）、`--format`の`json`/`yaml`対応、CSV列構成に加えJSON/YAMLのキー命名（camelCase）を反映。旧`*-csv`オプションの記載を削除。

- [ ] **Step 10: コード生成サマリ作成**
  - 対象（新規）: `aidlc-docs/construction/java-class-scanner/code/code-generation-summary.md`
  - 内容: 生成・修正したファイル一覧とFR/NFR/BRとの対応関係を記録するmarkdownサマリ（アプリケーションコードではなくドキュメントのみ）。

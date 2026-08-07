# Code Generation Summary — java-class-scanner

Code Generation（unit: java-class-scanner）で生成・修正したファイルと、`requirements.md`（FR/NFR）・
`functional-design/business-rules.md`（BR）との対応をまとめる。

## 修正したファイル（Brownfield）

| ファイル | 内容 |
|---|---|
| `build.gradle` | Jackson 3系（`tools.jackson.core:jackson-databind`, `tools.jackson.dataformat:jackson-dataformat-yaml`）と jqwik 1.10.1 を追加（NFR-2） |
| `src/main/java/cherry/classscanner/ClassScannerRunner.java` | DIベースの薄いオーケストレーターへ改修。CLIオプションのリネーム・追加(FR-5)、`--format`のjson/yaml対応(FR-2)、JSON/YAML集約書き込み(BR-9)、verbose拡張(FR-6/BR-13)を実装 |
| `README.md` / `README_en.md` | 新CLI仕様・出力形式の反映 |
| `CLAUDE.local.md` | 新アーキテクチャ・実装上の注意点（Jackson 3の落とし穴等）の反映 |

## 新規作成したファイル

### アプリケーションコード

| ファイル | 内容 | 対応FR/NFR/BR |
|---|---|---|
| `src/main/java/cherry/classscanner/model/ClassRecord.java` | クラス情報DTO（record） | FR-1 |
| `src/main/java/cherry/classscanner/model/MethodRecord.java` | メソッド情報DTO（record） | 既存機能の再設計 |
| `src/main/java/cherry/classscanner/model/FieldRecord.java` | フィールド情報DTO（record） | 既存機能の再設計 |
| `src/main/java/cherry/classscanner/model/ConstructorRecord.java` | コンストラクタ情報DTO（record） | 既存機能の再設計 |
| `src/main/java/cherry/classscanner/extract/RecordExtractor.java` | 抽出層。フィルタ・ソートロジック | BR-1, BR-2, BR-3 |
| `src/main/java/cherry/classscanner/output/RecordWriter.java` | 書式化層の共通インターフェース | NFR-1 |
| `src/main/java/cherry/classscanner/output/CsvRecordWriter.java` | CSV/TSV書式化 | BR-4, BR-5, BR-6, BR-7, BR-8 |
| `src/main/java/cherry/classscanner/output/JsonRecordWriter.java` | JSON書式化 | FR-2, FR-3, FR-4, BR-4, BR-6, BR-7 |
| `src/main/java/cherry/classscanner/output/YamlRecordWriter.java` | YAML書式化 | FR-2, FR-3, FR-4, BR-4, BR-6, BR-7 |
| `src/main/java/cherry/classscanner/config/JacksonConfig.java` | jsonMapper/yamlMapper Bean定義 | NFR-1（ロジカルコンポーネント） |

### テストコード

| ファイル | 内容 | 対応FR/NFR/BR/PBT |
|---|---|---|
| `src/test/java/cherry/classscanner/fixtures/{SampleClass,SampleInterface}.java` | ClassGraph実スキャン用フィクスチャ | テスト基盤 |
| `src/test/java/cherry/classscanner/extract/RecordExtractorTest.java` | 抽出層Example-basedテスト | FR-7, BR-1〜BR-3 |
| `src/test/java/cherry/classscanner/extract/RecordExtractorPropertyTest.java` | パッケージフィルタ不変条件 | PBT-03 |
| `src/test/java/cherry/classscanner/output/CsvRecordWriterTest.java` | CSV/TSV書式化Example-basedテスト | FR-7, BR-4〜BR-8 |
| `src/test/java/cherry/classscanner/output/JsonRecordWriterTest.java` | JSON書式化Example-basedテスト | FR-2〜FR-4, BR-4, BR-6, BR-7 |
| `src/test/java/cherry/classscanner/output/YamlRecordWriterTest.java` | YAML書式化Example-basedテスト | FR-2〜FR-4, BR-4, BR-6, BR-7 |
| `src/test/java/cherry/classscanner/output/JacksonRoundTripPropertyTest.java` | JSON/YAML往復変換 | PBT-02 |
| `src/test/java/cherry/classscanner/ClassScannerRunnerTest.java` | オーケストレーションExample-basedテスト | FR-1〜FR-7 |
| `src/test/java/cherry/classscanner/ClassScannerRunnerPropertyTest.java` | CSV列構成不変条件 | PBT-03 |

### 設定

| ファイル | 内容 |
|---|---|
| `.gitignore` | jqwikのローカル実行履歴（`.jqwik-database`）を除外対象に追加 |

## FR/NFR網羅状況

| ID | 内容 | 状態 |
|---|---|---|
| FR-1 | クラス一覧のファイル出力 | 実装済み（`--classes-output`） |
| FR-2 | JSON/YAML出力の追加 | 実装済み（`--format=json\|yaml`） |
| FR-3 | 複数値項目のarray化(JSON/YAML)・デリミタ結合維持(CSV) | 実装済み |
| FR-4 | JSON/YAMLキーの英語camelCase | 実装済み |
| FR-5 | CLIオプションのリネーム（旧名廃止） | 実装済み |
| FR-6 | verboseへの修飾子・クラスアノテーション追加 | 実装済み |
| FR-7 | 既存機能へのテスト整備 | 実装済み（`ClassScannerRunnerTest`等） |
| FR-8 | 適宜コメント追加 | 実装済み（各クラスのJavadoc/インラインコメント） |
| NFR-1 | 抽出/書式化分離リファクタリング | 実装済み |
| NFR-2 | Jackson/jqwik依存追加 | 実装済み |
| NFR-3 | 後方互換性なし | 実装済み（旧オプション名は認識されない） |
| NFR-4 | PBT方針(Partial: PBT-02/03/07/08/09) | 実装済み（PBT-02, PBT-03のテストを実装。PBT-07/08/09はjqwikのジェネレータ品質・シュリンク機能・フレームワーク選定として自動的に充足） |

## 既知の制約・後続課題
- `ClassScannerRunnerTest`の`run_ioErrorWritingOutput_exitCodeIsOne`は、出力先ディレクトリ不在によるIOExceptionでのexitCode検証のみを行っており、BR-12の「複数入力中の1件だけがエラーになりベストエフォート書き込みされる」シナリオそのものは、信頼性の高い形で再現するテスト構築が難しく個別のテストとしては未実装（try/finally構造による設計上の担保に留まる）。
- YAML/JSON出力のpretty-print・null表現等の細部は実際のJackson出力を都度確認しながら実装しており、将来Jacksonのバージョンが上がった際は出力フォーマットの変化がないか確認が望ましい。

## 追記: Code Generation承認後の簡略化（BR-8/BR-9改訂）

ユーザーレビュー時に「CSV/TSVも最後にまとめて出力する方がシンプルになるか」という質問があり、既存のCSV/TSV逐次書き込み挙動との互換性維持は不要と判断されたため、以下の簡略化を実施した。

- `RecordWriter<T>.write()`から`append`引数を削除（全フォーマット共通で常に新規1回書き込みとなったため不要）。
- `ClassScannerRunner`から`csvFilesCreated`（追記制御用の状態管理）と、CSV/TSV用の逐次書き込みモード・JSON/YAML用の集約書き込みモードという二重の分岐ロジックを削除。全フォーマットが単一の`Aggregation`集約→1回書き込みという経路に統一され、`ClassScannerRunner`の行数が正味約50行減少した。
- `functional-design/business-rules.md`（BR-8/BR-9統合、BR-12一般化）、`nfr-requirements.md`（NFR-R1の適用範囲拡大）、`inception/application-design/services.md`（非対称性の解消を追記）、`CLAUDE.local.md`・`README.md`・`README_en.md`（該当記述の更新）を連動して更新した。
- トレードオフ: CSV/TSVが従来持っていた「大規模スキャン時の省メモリ・ストリーミング特性」は失われる。ユーザーが明示的に許容した判断である。
- `./gradlew build`で全38テスト成功を再確認済み。

# Application Design（統合ドキュメント）

本ドキュメントは`components.md`, `component-methods.md`, `services.md`, `component-dependency.md`の要点を統合したものです。詳細は各ファイルを参照してください。

## 設計方針の要約（Application Design Q1-Q5の決定事項）

1. **パッケージ構成**（Q1=A）: `cherry.classscanner`（既存、Main+オーケストレーション）, `cherry.classscanner.model`（DTO）, `cherry.classscanner.extract`（抽出）, `cherry.classscanner.output`（書式化）に分割。
2. **DTOの実装**（Q2=A）: `ClassRecord`/`MethodRecord`/`FieldRecord`/`ConstructorRecord`をJava 25の`record`型として実装。複数値項目は`List`（`parameterAnnotations`のようなネスト構造は`List<List<String>>`）として保持し、CSV/JSON/YAMLどちらの書式化にも対応できる共通表現とする。
3. **JSON/YAML複数入力集約**（Q3=A）: 全入力ファイルのレコードを`sourcePath`フィールドで区別しつつ、フラットな1つのJSON配列/YAMLシーケンスにまとめる。CSVのような「ファイルごとの逐次追記」ではなく、全対象処理後に1回だけ書き出す。
4. **Writer抽象化**（Q4=A）: `RecordWriter<T>`共通インターフェース + `CsvRecordWriter`/`JsonRecordWriter`/`YamlRecordWriter`実装（Strategyパターン）。`--format`値により実行時に選択。
5. **オーケストレーション責務**（Q5=A）: `ClassScannerRunner`は引数解析・ファイル列挙・スキャン実行・抽出/書式化呼び出しの制御のみを担当し、DTO変換・書式化ロジックは持たない。

## コンポーネント一覧（詳細は`components.md`）
- `Main`（既存）
- `ClassScannerRunner`（既存、責務縮小）
- DTO群: `ClassRecord`, `MethodRecord`, `FieldRecord`, `ConstructorRecord`（新設）
- `RecordExtractor`（新設）
- `RecordWriter<T>`インターフェース + `CsvRecordWriter`/`JsonRecordWriter`/`YamlRecordWriter`（新設）

## コンポーネントメソッド（詳細は`component-methods.md`）
- DTOのフィールド定義（各レコードの保持項目）
- `RecordExtractor`の抽出メソッド（クラス/メソッド/フィールド/コンストラクタごとに1メソッド）
- `RecordWriter<T>.write(List<T>, Path, Charset, boolean append)`

## サービス層（詳細は`services.md`）
- `ClassScannerRunner`が担う処理フロー（引数解析→列挙→スキャン→抽出→書式に応じた出力タイミング制御→コンソール出力→終了コード）
- CSV/TSV（逐次書き込み）とJSON/YAML（集約後に1回書き込み）の非対称性はオーケストレーション層に閉じ込める

## コンポーネント依存関係（詳細は`component-dependency.md`）
- `ClassScannerRunner` → `RecordExtractor` → DTO群 ← `RecordWriter<T>`実装群（CSV: Apache Commons CSV、JSON/YAML: Jackson）
- 全て同一JVM内の直接呼び出し。外部サービス・ネットワーク依存なし。

## 未確定事項・後続ステージへの引き継ぎ
以下はFunctional Design（Construction Phase）で詳細化する:
- `ClassScannerRunner`内の具体的なメソッド分割
- ソート順・フィルタリングの具体的な比較条件（既存ロジックの踏襲を基本とする）
- charset/format不正値時のフォールバック処理の詳細（既存ロジックの踏襲を基本とする）
- PBT対象の不変条件の具体的なテストケース設計

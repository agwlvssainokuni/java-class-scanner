# Components

## パッケージ構成（承認済み: Application Design Question 1 = A）

```
cherry.classscanner            既存パッケージ。Main + オーケストレーション用ClassScannerRunnerのみ残す
cherry.classscanner.model      新設。DTO（レコード型）
cherry.classscanner.extract    新設。ClassGraph情報 → DTO への抽出ロジック
cherry.classscanner.output     新設。DTO → CSV/TSV/JSON/YAML への書式化(Writer)ロジック
```

## コンポーネント一覧

### Main（既存、変更なし）
- **Purpose**: Spring Bootアプリケーションのエントリポイント。
- **Responsibilities**: 起動、終了コードのOSへの返却。
- **Package**: `cherry.classscanner`

### ClassScannerRunner（既存、責務縮小）
- **Purpose**: CLIオーケストレーター。
- **Responsibilities**: コマンドライン引数解析、対象ファイル/ディレクトリの列挙、ClassGraphによるスキャン実行、抽出層(`RecordExtractor`)と書式化層(`RecordWriter`)の呼び出し順序制御、コンソール出力（標準/`--verbose`）、終了コード管理。実際のDTO変換・ファイル書式化ロジックは持たない（承認済み: Application Design Question 5 = A）。
- **Package**: `cherry.classscanner`

### DTO群（新設）
`ClassRecord`, `MethodRecord`, `FieldRecord`, `ConstructorRecord` — Java 25の`record`型として実装する（承認済み: Application Design Question 2 = A）。不変・シンプルなデータ保持のみを責務とし、ロジックを持たない。
- **Purpose**: 抽出層と書式化層の間で受け渡しする中間データモデル。CSV/JSON/YAMLいずれの書式化にも使える共通表現。
- **Responsibilities**: フィールド保持のみ（フィールド詳細は`component-methods.md`参照）。
- **Package**: `cherry.classscanner.model`

### RecordExtractor（新設）
- **Purpose**: ClassGraphの`ClassInfo`/`MethodInfo`/`FieldInfo`/`ConstructorInfo`をDTOへ変換する。
- **Responsibilities**: 型ごとの抽出・ソート・フィルタリング（既存の`isRegularMethod`によるメソッドフィルタ、クラス名/メソッド名/フィールド名順・コンストラクタ引数数順のソートを踏襲）。
- **Package**: `cherry.classscanner.extract`

### RecordWriter\<T\>（新設、インターフェース）とその実装（CSV/TSV用・JSON用・YAML用）
- **Purpose**: DTOのリストを指定フォーマットでファイルへ書き出す（承認済み: Application Design Question 4 = A、Strategyパターン）。
- **Responsibilities**:
  - CSV/TSV用実装: 既存の`CSVPrinter`ベースの出力を踏襲。複数値項目（実装インターフェース/引数/各種アノテーション）はデリミタ結合の単一カラムとして出力（FR-3）。ファイルごとのヘッダー管理・追記制御（既存の`csvFilesCreated`相当の仕組み）を担う。
  - JSON用/YAML用実装: Jackson（`jackson-databind`/`jackson-dataformat-yaml`）を用い、複数値項目をネイティブなarrayとして出力（FR-3）。キー名は英語camelCase（FR-4）。複数入力ファイルの集約は、全入力から集めたDTOリストをフラットな1配列としてまとめて1回で書き出す（承認済み: Application Design Question 3 = A）。CSVのような「ファイルごとの追記」は行わない。
- **Package**: `cherry.classscanner.output`

## コンポーネント間関係サマリ
`ClassScannerRunner` → `RecordExtractor`（抽出） → `RecordWriter<T>`実装（書式化）という一方向の呼び出しフロー。詳細は`component-dependency.md`を参照。

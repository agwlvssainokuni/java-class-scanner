# Component Methods

**Note**: ここではメソッドシグネチャと入出力の型を定義する。詳細なビジネスルール（ソート順の具体的な比較条件、フォールバック条件の詳細等）はConstruction PhaseのFunctional Designで確定する。

## DTO（`cherry.classscanner.model`）

### `ClassRecord`（record）
| フィールド | 型 | 備考 |
|---|---|---|
| `sourcePath` | `String` | 入力元（JAR/ディレクトリ）のパス |
| `className` | `String` | 完全修飾クラス名 |
| `type` | `String` | `Class`/`Interface`/`Abstract Class`/`Enum`/`Annotation` |
| `superclass` | `String`（nullable） | 親クラス名。存在しない場合は`null` |
| `interfaces` | `List<String>` | 実装インターフェース名（複数可） |
| `packageName` | `String` | パッケージ名 |
| `modifiers` | `String` | ClassGraphの`getModifiersStr()`をそのまま保持（既存CSV挙動を踏襲し分割しない） |
| `classAnnotations` | `List<String>` | クラスアノテーション名（複数可、FR-1/Follow-up C） |

### `MethodRecord`（record）
| フィールド | 型 | 備考 |
|---|---|---|
| `sourcePath` | `String` | |
| `className` | `String` | |
| `methodName` | `String` | |
| `returnType` | `String` | |
| `parameters` | `List<String>` | 引数の型シグネチャ（複数可） |
| `modifiers` | `String` | |
| `isStatic` | `boolean` | |
| `methodAnnotations` | `List<String>` | メソッドアノテーション名（複数可） |
| `parameterAnnotations` | `List<List<String>>` | 引数ごとのアノテーション名リスト（引数数分の要素、各要素はその引数のアノテーション名リスト） |

### `FieldRecord`（record）
| フィールド | 型 | 備考 |
|---|---|---|
| `sourcePath` | `String` | |
| `className` | `String` | |
| `fieldName` | `String` | |
| `fieldType` | `String` | |
| `modifiers` | `String` | |
| `isStatic` | `boolean` | |
| `fieldAnnotations` | `List<String>` | |

### `ConstructorRecord`（record）
| フィールド | 型 | 備考 |
|---|---|---|
| `sourcePath` | `String` | |
| `className` | `String` | |
| `parameters` | `List<String>` | |
| `modifiers` | `String` | |
| `constructorAnnotations` | `List<String>` | |
| `parameterAnnotations` | `List<List<String>>` | |

## `RecordExtractor`（`cherry.classscanner.extract`）

| メソッド | 入力 | 出力 | 目的 |
|---|---|---|---|
| `extractClasses` | `String sourcePath, List<ClassInfo> classes` | `List<ClassRecord>` | クラス一覧の抽出（クラス名順ソート） |
| `extractMethods` | `String sourcePath, List<ClassInfo> classes` | `List<MethodRecord>` | 全クラスの通常メソッド抽出（既存`isRegularMethod`フィルタ + メソッド名順ソート） |
| `extractFields` | `String sourcePath, List<ClassInfo> classes` | `List<FieldRecord>` | 全クラスのフィールド抽出（フィールド名順ソート） |
| `extractConstructors` | `String sourcePath, List<ClassInfo> classes` | `List<ConstructorRecord>` | 全クラスのコンストラクタ抽出（引数数順ソート） |

## `RecordWriter<T>`（`cherry.classscanner.output`、インターフェース）

| メソッド | 入力 | 出力 | 目的 |
|---|---|---|---|
| `write` | `List<T> records, Path outputPath, Charset charset, boolean append` | `void`（`IOException`をスロー） | 指定フォーマットでレコード群をファイルへ書き出す |

### 実装クラス
| クラス | 対応フォーマット | `append`の扱い |
|---|---|---|
| `CsvRecordWriter<T>` | CSV, TSV（`CSVFormat`で切替） | `true`の場合はヘッダーなしで追記（既存の複数入力ソースパス集約を実現） |
| `JsonRecordWriter<T>` | JSON | 呼び出し側（`ClassScannerRunner`）が全入力を集約した完全なリストを1回だけ渡す運用とし、`append`は使用しない（常に新規書き込み） |
| `YamlRecordWriter<T>` | YAML | 同上 |

**設計メモ**: CSV/TSVは既存通りファイルごとの逐次書き込み（ストリーミング・追記）を維持する一方、JSON/YAMLは構造化ドキュメントであるため全入力処理完了後にまとめて1回で書き出す（承認済み: Application Design Question 3 = A）。この「いつWriterを呼ぶか」の制御は`ClassScannerRunner`（オーケストレーション層）の責務とする。

## `ClassScannerRunner`（`cherry.classscanner`、責務縮小後）

| メソッド | 入力 | 出力 | 目的 |
|---|---|---|---|
| `run` | `ApplicationArguments args` | `void` | エントリーポイント。既存の引数解釈・使用方法表示・終了コード制御ロジックを維持 |
| `getExitCode` | なし | `int` | 既存通り |
| （内部）オーケストレーションメソッド群 | — | — | 対象ファイル列挙→ClassGraphスキャン→`RecordExtractor`呼び出し→（CSV/TSVは逐次、JSON/YAMLは集約後に1回）`RecordWriter`呼び出し、という制御フローを実装。具体的なメソッド分割はFunctional Designで確定する |

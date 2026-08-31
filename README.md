# Java Class Scanner

Java Class Scannerは、JARファイルやディレクトリ内のJavaクラスファイルを解析し、クラス情報を抽出・出力するコマンドラインツールです。

## 機能

- JARファイルやディレクトリ内のJavaクラスの詳細情報を取得
- クラス、メソッド、フィールド、コンストラクタ情報のCSV/TSV/JSON/YAML出力
- アノテーション情報の抽出・出力対応（クラス、メソッド、フィールド、コンストラクタ、引数アノテーション）
- 複数ファイルスキャン時の結果集約（ソースパス追跡付き）
- アルファベット順・引数数順でのソート済み出力
- パッケージ名による絞り込み機能
- 複数の文字エンコーディング対応
- 詳細表示モード（クラスの修飾子・アノテーションも表示）

## 必要な環境

- Java 25 以上
- Gradle 9.x（ビルド時）

## ビルド方法

```bash
./gradlew build
```

実行可能JARファイルの作成：
```bash
./gradlew bootJar
```

## デモ

`./gradlew bootJar`実行後、以下でデモを実行できます。`demo/src`配下の小さなサンプルプロジェクト（図書館ドメインモデル）をその場でコンパイルし、コンソール出力・CSV/TSV/JSON/YAML出力・複数入力集約・不正値フォールバックなど主要機能を一通り実行して結果を表示します。

```bash
./demo.sh
```

## 使用方法

### 基本的な使用方法

```bash
java -jar build/libs/java-class-scanner-*.jar <file|directory>...
```

### オプション

| オプション | 説明 |
|-----------|------|
| `--verbose` | クラスの詳細情報を表示 |
| `--package=<package>` | 指定したパッケージ名でフィルタリング |
| `--classes-output=<file>` | クラス一覧をファイルに出力 |
| `--methods-output=<file>` | メソッド情報をファイルに出力 |
| `--fields-output=<file>` | フィールド情報をファイルに出力 |
| `--constructors-output=<file>` | コンストラクタ情報をファイルに出力 |
| `--format=<format>` | 出力形式 (csv, tsv, json, yaml のいずれか。デフォルト: csv) |
| `--charset=<charset>` | 出力ファイルの文字エンコーディング (デフォルト: UTF-8) |
| `--quiet` | 標準出力を抑制 |

### 使用例

1. JARファイルの基本的なクラス一覧表示：
```bash
java -jar java-class-scanner.jar myapp.jar
```

2. 詳細情報付きでクラス情報を表示：
```bash
java -jar java-class-scanner.jar --verbose myapp.jar
```

3. 特定パッケージのクラスのみ表示：
```bash
java -jar java-class-scanner.jar --package=com.example myapp.jar
```

4. メソッド情報をCSVファイルに出力：
```bash
java -jar java-class-scanner.jar --methods-output=methods.csv myapp.jar
```

5. 複数の出力オプションを組み合わせ：
```bash
java -jar java-class-scanner.jar \
  --classes-output=classes.tsv \
  --methods-output=methods.tsv \
  --fields-output=fields.tsv \
  --constructors-output=constructors.tsv \
  --format=tsv \
  --charset=Shift_JIS \
  myapp.jar
```

6. ディレクトリ内のクラスファイルを解析：
```bash
java -jar java-class-scanner.jar /path/to/classes
```

7. 複数のJARファイルを一括解析してCSVに集約：
```bash
java -jar java-class-scanner.jar --methods-output=all-methods.csv app1.jar app2.jar lib.jar
```

8. JSON形式でクラス一覧とメソッド情報を出力：
```bash
java -jar java-class-scanner.jar \
  --classes-output=classes.json \
  --methods-output=methods.json \
  --format=json \
  myapp.jar
```

9. YAML形式で出力：
```bash
java -jar java-class-scanner.jar --methods-output=methods.yaml --format=yaml myapp.jar
```

## 出力形式

CSV/TSV出力の列構成（日本語ヘッダー）と、JSON/YAML出力のキー構成（英語camelCase）は対応しています。
複数値を持つ項目（実装インターフェース、引数、各種アノテーション等）は、CSV/TSVではデリミタ区切りの
1カラムとして出力されますが、JSON/YAMLではネイティブな配列（引数ごとのアノテーションは配列の配列）
として出力されます。値が存在しない項目（クラスの親クラス等）は、CSV/TSVでは空文字列、JSON/YAMLでは
明示的な`null`として表現されます。

### クラス情報 (classes.csv)
| カラム (CSV) | キー (JSON/YAML) | 説明 |
|--------|--------|------|
| ソースパス | sourcePath | スキャン対象のファイル/ディレクトリパス |
| クラス名 | className | フルクラス名 |
| 型 | type | Class / Interface / Abstract Class / Enum / Annotation |
| 親クラス | superclass | 親クラス名（存在しない場合は空/null） |
| 実装インターフェース | interfaces | 実装しているインターフェース |
| パッケージ | packageName | パッケージ名 |
| 修飾子 | modifiers | アクセス修飾子等 |
| クラスアノテーション | classAnnotations | クラスに付与されたアノテーション |

### メソッド情報 (methods.csv)
| カラム (CSV) | キー (JSON/YAML) | 説明 |
|--------|--------|------|
| ソースパス | sourcePath | スキャン対象のファイル/ディレクトリパス |
| クラス名 | className | フルクラス名 |
| メソッド名 | methodName | メソッド名 |
| 返却値 | returnType | 戻り値の型 |
| 引数 | parameters | 引数の型リスト |
| 修飾子 | modifiers | アクセス修飾子等 |
| IsStatic | isStatic | static メソッドかどうか |
| メソッドアノテーション | methodAnnotations | メソッドに付与されたアノテーション |
| 引数アノテーション | parameterAnnotations | 各引数に付与されたアノテーション |

### フィールド情報 (fields.csv)
| カラム (CSV) | キー (JSON/YAML) | 説明 |
|--------|--------|------|
| ソースパス | sourcePath | スキャン対象のファイル/ディレクトリパス |
| クラス名 | className | フルクラス名 |
| フィールド名 | fieldName | フィールド名 |
| フィールド型 | fieldType | フィールドの型 |
| 修飾子 | modifiers | アクセス修飾子等 |
| IsStatic | isStatic | static フィールドかどうか |
| フィールドアノテーション | fieldAnnotations | フィールドに付与されたアノテーション |

### コンストラクタ情報 (constructors.csv)
| カラム (CSV) | キー (JSON/YAML) | 説明 |
|--------|--------|------|
| ソースパス | sourcePath | スキャン対象のファイル/ディレクトリパス |
| クラス名 | className | フルクラス名 |
| 引数 | parameters | 引数の型リスト |
| 修飾子 | modifiers | アクセス修飾子等 |
| コンストラクタアノテーション | constructorAnnotations | コンストラクタに付与されたアノテーション |
| 引数アノテーション | parameterAnnotations | 各引数に付与されたアノテーション |

### 注記
- 複数ファイル・ディレクトリを指定した場合、すべての結果が1つの出力ファイルに集約されます（フォーマットを問わず、全対象の処理完了後にまとめて1回だけ出力されます）
- ソースパス列（キー）により、各クラスがどのファイル/ディレクトリから抽出されたかを追跡できます
- アノテーション情報には完全修飾クラス名が含まれます
- 出力は自動的にソートされます（クラス名、メソッド名、フィールド名：アルファベット順、コンストラクタ：引数数順）
- 内部メソッド（`<init>`, `<clinit>`, ラムダメソッド）は出力から除外されます
- 該当データが0件でも出力ファイルは生成されます（CSV/TSVはヘッダーのみ、JSON/YAMLは空配列/空シーケンス）

## 開発

### テストの実行
```bash
./gradlew test
```

### 開発用実行
```bash
./gradlew run --args="<arguments>"
```

## 技術仕様

- **フレームワーク**: Spring Boot 4.1.1
- **Java バージョン**: Java 25
- **主要ライブラリ**:
  - ClassGraph 4.8.194 (クラス解析)
  - Apache Commons CSV 1.14.1 (CSV/TSV出力)
  - Jackson 3系 (`tools.jackson`, JSON/YAML出力)
  - Apache Commons Lang3 (ユーティリティ)
  - jqwik 1.10.1 (Property-Based Testing、テストのみ)

### アーキテクチャの特徴
- **モダンJava機能**: Java 25の`toList()`、record型、メソッド参照、switch式を活用
- **抽出/書式化の分離**: ClassGraph情報をDTO（record）へ変換する抽出層と、DTOをCSV/TSV/JSON/YAMLへ変換する書式化層(Strategyパターン)を分離
- **Spring DIベースの構成**: 各コンポーネントをSpring管理Beanとしてコンストラクタインジェクションで組み立て
- **集約書き込み**: 全フォーマット共通で、全入力処理完了後に1回だけファイルへ書き込み
- **包括的ソート**: 全出力の一貫したソート処理

## ライセンス

Apache License 2.0
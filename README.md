# Java Class Scanner

Java Class Scannerは、JARファイルやディレクトリ内のJavaクラスファイルを解析し、クラス情報を抽出・出力するコマンドラインツールです。

## 機能

- JARファイルやディレクトリ内のJavaクラスの詳細情報を取得
- メソッド、フィールド、コンストラクタ情報のCSV/TSV出力
- パッケージ名による絞り込み機能
- 複数の文字エンコーディング対応
- 詳細表示モード

## 必要な環境

- Java 21 以上
- Gradle 8.x（ビルド時）

## ビルド方法

```bash
./gradlew build
```

実行可能JARファイルの作成：
```bash
./gradlew bootJar
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
| `--methods-csv=<file>` | メソッド情報をCSVファイルに出力 |
| `--fields-csv=<file>` | フィールド情報をCSVファイルに出力 |
| `--constructors-csv=<file>` | コンストラクタ情報をCSVファイルに出力 |
| `--format=<format>` | 出力形式 (csv または tsv、デフォルト: csv) |
| `--charset=<charset>` | CSVファイルの文字エンコーディング (デフォルト: UTF-8) |
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
java -jar java-class-scanner.jar --methods-csv=methods.csv myapp.jar
```

5. 複数の出力オプションを組み合わせ：
```bash
java -jar java-class-scanner.jar \
  --methods-csv=methods.csv \
  --fields-csv=fields.csv \
  --constructors-csv=constructors.csv \
  --format=tsv \
  --charset=Shift_JIS \
  myapp.jar
```

6. ディレクトリ内のクラスファイルを解析：
```bash
java -jar java-class-scanner.jar /path/to/classes
```

## CSV出力形式

### メソッド情報 (methods.csv)
| カラム | 説明 |
|--------|------|
| クラス名 | フルクラス名 |
| メソッド名 | メソッド名 |
| 返却値 | 戻り値の型 |
| 引数 | 引数の型リスト |
| 修飾子 | アクセス修飾子等 |
| IsStatic | static メソッドかどうか |

### フィールド情報 (fields.csv)
| カラム | 説明 |
|--------|------|
| クラス名 | フルクラス名 |
| フィールド名 | フィールド名 |
| フィールド型 | フィールドの型 |
| 修飾子 | アクセス修飾子等 |
| IsStatic | static フィールドかどうか |

### コンストラクタ情報 (constructors.csv)
| カラム | 説明 |
|--------|------|
| クラス名 | フルクラス名 |
| 引数 | 引数の型リスト |
| 修飾子 | アクセス修飾子等 |

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

- **フレームワーク**: Spring Boot 3.5.4
- **Java バージョン**: Java 21
- **主要ライブラリ**:
  - ClassGraph 4.8.165 (クラス解析)
  - Apache Commons CSV 1.10.0 (CSV出力)
  - Apache Commons Lang3 (ユーティリティ)

## ライセンス

Apache License 2.0
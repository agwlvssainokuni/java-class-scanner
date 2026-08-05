# API Documentation

**Note**: 本プロジェクトはREST/Web APIを公開しないスタンドアロンCLIである。ここでは「API」として、コマンドラインインターフェース仕様と主要な内部クラスのpublicシグネチャを記載する。

## REST APIs

該当なし（Webエンドポイントを持たない）。

## Command Line Interface（実質的な公開APIとして機能）

### 起動コマンド
- **形式**: `java -jar java-class-scanner.jar [options] <file|directory>...`
- **Purpose**: 1つ以上のJARファイル/ディレクトリをスキャンし、クラス情報をコンソール/CSVへ出力する。
- **Request（コマンドライン引数）**:
  - 非オプション引数（可変長）: スキャン対象のJARファイルパスまたはディレクトリパス。存在しないパスは無視される。
  - `--verbose`（フラグ）: クラスの型・親クラス・実装インターフェース・フィールド・メソッド・コンストラクタを詳細表示。
  - `--package=<package>`（値、複数指定可）: 指定したパッケージ名で前方一致フィルタリング。
  - `--methods-csv=<file>`（値）: メソッド情報をCSV/TSVへ出力するファイルパス。
  - `--fields-csv=<file>`（値）: フィールド情報をCSV/TSVへ出力するファイルパス。
  - `--constructors-csv=<file>`（値）: コンストラクタ情報をCSV/TSVへ出力するファイルパス。
  - `--format=<csv|tsv>`（値、デフォルト `csv`）: CSV出力フォーマット。不明な値は警告の上 `csv` にフォールバック。
  - `--charset=<charset>`（値、デフォルト `UTF-8`）: CSV出力の文字エンコーディング。不正な値は警告の上 `UTF-8` にフォールバック。
  - `--quiet`（フラグ）: 標準出力（使用方法・進捗・警告）を抑制。
- **Response（出力）**:
  - 標準出力（ログ）: クラス一覧（標準/詳細モード）、処理進捗、警告メッセージ。
  - CSV/TSVファイル: 指定されたオプションに応じてメソッド/フィールド/コンストラクタ情報を出力（複数入力時は追記集約）。
  - プロセス終了コード: `0`（正常終了・非オプション引数なしの場合を含む）、`1`（`IOException` 発生時）。

## Internal APIs

### `cherry.classscanner.Main`
- **Methods**:
  - `public static void main(@Nonnull String[] args)` — プロセスのエントリポイント。`doMain()` の戻り値で `System.exit()` する。
  - `private static int doMain(@Nonnull String[] args)` — `SpringApplication.run()` でコンテキストを起動し、`SpringApplication.exit()` の戻り値（終了コード）を返す。
- **Parameters**: `args` — JVM起動時のコマンドライン引数配列。
- **Return Types**: `doMain` はプロセス終了コード（`int`）を返す。

### `cherry.classscanner.ClassScannerRunner`（`@Component`, `implements ApplicationRunner, ExitCodeGenerator`）
- **Methods**:
  - `public void run(@Nonnull ApplicationArguments args)` — CLIのメインロジック。非オプション引数が空なら使用方法を表示して終了、それ以外は `processJarFiles()` を呼び出し、`IOException` 発生時は `exitCode=1` を設定。
  - `public int getExitCode()` — 保持している終了コードを返す（`ExitCodeGenerator` 実装）。
- **Parameters**: `args` — Spring Bootが解析したオプション/非オプション引数を保持する `ApplicationArguments`。
- **Return Types**: `run` は戻り値なし（副作用として `exitCode` フィールドとコンソール/ファイル出力を更新）。`getExitCode` は `int`。

## Data Models

### スキャン結果の内部表現（ClassGraphライブラリ由来、本プロジェクトが直接定義するモデルクラスはなし）
- **Fields**: `ClassInfo`（クラス名・パッケージ名・修飾子・親クラス・実装インターフェース）、`MethodInfo`（名前・返却型・引数・修飾子・アノテーション）、`FieldInfo`（名前・型・修飾子・アノテーション）、`ConstructorInfo`（引数・修飾子・アノテーション）、`MethodParameterInfo`（型・アノテーション）、`AnnotationInfo`（名前）。
- **Relationships**: `ClassInfo` が `MethodInfo` / `FieldInfo` / `ConstructorInfo` のコレクションを保持し、各 `MethodInfo`/`ConstructorInfo` が `MethodParameterInfo[]` を保持する。
- **Validation**: 本プロジェクト独自のバリデーションは行わず、ClassGraphが解析したバイトコード情報をそのまま整形・出力する。

### CSV出力レコード（本プロジェクト定義の出力スキーマ）
- **メソッドCSV列**: `ソースパス, クラス名, メソッド名, 返却値, 引数, 修飾子, IsStatic, メソッドアノテーション, 引数アノテーション`
- **フィールドCSV列**: `ソースパス, クラス名, フィールド名, フィールド型, 修飾子, IsStatic, フィールドアノテーション`
- **コンストラクタCSV列**: `ソースパス, クラス名, 引数, 修飾子, コンストラクタアノテーション, 引数アノテーション`
- **Relationships**: いずれも1行=1メンバー（メソッド/フィールド/コンストラクタ）に対応し、`ソースパス` 列で入力元ファイル/ディレクトリを識別する。
- **Validation**: なし（抽出した文字列をそのまま出力）。

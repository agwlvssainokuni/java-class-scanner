#!/usr/bin/env bash
#
# java-class-scanner デモスクリプト
#
# demo/src配下の小さなサンプルプロジェクト(com.example.library)をコンパイルし、
# java-class-scannerでスキャンして各種オプションの実行結果を確認できます。
#
# 実行前に以下を実行してJARをビルドしてください:
#   ./gradlew bootJar
#
# 使い方:
#   ./demo.sh
#
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR="$(find "$ROOT_DIR/build/libs" -name 'java-class-scanner-*.jar' ! -name '*-plain.jar' 2>/dev/null | head -1)"
if [ -z "${JAR:-}" ]; then
  JAR="$ROOT_DIR/build/libs/java-class-scanner.jar"
fi
if [ ! -f "$JAR" ]; then
  echo "実行可能JARが見つかりません。先に ./gradlew bootJar を実行してください。" >&2
  exit 1
fi

# --- デモ対象プロジェクト(com.example.library)をコンパイルし、JARも作成する ---
DEMO_SRC="$ROOT_DIR/demo/src"
DEMO_BUILD="$ROOT_DIR/demo/build"
DEMO_CLASSES="$DEMO_BUILD/classes"
DEMO_JAR="$DEMO_BUILD/library-demo.jar"

rm -rf "$DEMO_BUILD"
mkdir -p "$DEMO_CLASSES"
javac -d "$DEMO_CLASSES" $(find "$DEMO_SRC" -name '*.java')
jar --create --file "$DEMO_JAR" -C "$DEMO_CLASSES" .

DEMO_DIR="$(mktemp -d)"
trap 'rm -rf "$DEMO_DIR"' EXIT

section() {
  echo
  echo "================================================================"
  echo "  $1"
  echo "================================================================"
}

show_file() {
  echo "--- $(basename "$1") の内容 ---"
  cat "$1"
  echo
}

section "0. デモ対象プロジェクト: demo/src/com/example/library をコンパイル済み"
echo "  ディレクトリ: $DEMO_CLASSES"
echo "  JAR        : $DEMO_JAR"
echo "  (Book/PaperBook/EBook/Author/Borrowableの5クラスから成る小さな図書館ドメインモデル)"

section "1. 基本: デモプロジェクトのクラス一覧をコンソール表示"
java -jar "$JAR" "$DEMO_CLASSES"

section "2. --verbose: 詳細情報を表示(修飾子・アノテーション含む、EBookの@Deprecatedに注目)"
java -jar "$JAR" --verbose --package=com.example.library.EBook "$DEMO_CLASSES"

section "3. --classes-output: クラス一覧をCSVへ出力"
java -jar "$JAR" --quiet \
  --classes-output="$DEMO_DIR/classes.csv" "$DEMO_CLASSES"
show_file "$DEMO_DIR/classes.csv"

section "4. --format=json --methods-output: Bookクラスのメソッド情報をJSONへ出力"
java -jar "$JAR" --quiet --package=com.example.library.Book \
  --methods-output="$DEMO_DIR/methods.json" --format=json "$DEMO_CLASSES"
show_file "$DEMO_DIR/methods.json"

section "5. --format=yaml --fields-output: フィールド情報をYAMLへ出力(静的フィールドUNKNOWN_YEARに注目)"
java -jar "$JAR" --quiet \
  --fields-output="$DEMO_DIR/fields.yaml" --format=yaml "$DEMO_CLASSES"
show_file "$DEMO_DIR/fields.yaml"

section "6. --format=tsv --constructors-output: コンストラクタ情報をTSVへ出力"
java -jar "$JAR" --quiet \
  --constructors-output="$DEMO_DIR/constructors.tsv" --format=tsv "$DEMO_CLASSES"
show_file "$DEMO_DIR/constructors.tsv"

section "7. 複数入力の集約: 同じクラス群をディレクトリとJARの両方から指定"
echo "  (ソースパス列/sourcePathフィールドで、ディレクトリ由来かJAR由来かを区別できる)"
java -jar "$JAR" --quiet --package=com.example.library.Author \
  --classes-output="$DEMO_DIR/multi.csv" "$DEMO_CLASSES" "$DEMO_JAR"
show_file "$DEMO_DIR/multi.csv"

section "8. 不正な --format 値: csvへ自動フォールバック(警告メッセージに注目)"
java -jar "$JAR" --package=com.example.library.Author --format=xml \
  --fields-output="$DEMO_DIR/fallback.out" "$DEMO_CLASSES"
show_file "$DEMO_DIR/fallback.out"

section "9. 不正な --charset 値: UTF-8へ自動フォールバック(警告メッセージに注目)"
java -jar "$JAR" --package=com.example.library.Author --charset=BOGUS \
  --fields-output="$DEMO_DIR/charset-fallback.csv" "$DEMO_CLASSES"

section "10. 該当データ0件でも出力ファイルは生成される(--quiet併用)"
java -jar "$JAR" --quiet --package=no.such.package \
  --classes-output="$DEMO_DIR/empty.json" --format=json "$DEMO_CLASSES"
echo "(該当クラスなしのため標準出力はなし。生成されたファイルの中身)"
show_file "$DEMO_DIR/empty.json"

section "11. 引数なし実行: 使用方法(usage)を表示"
java -jar "$JAR" || true

echo
echo "デモ完了。一時出力ファイルは終了時に自動削除されます: $DEMO_DIR"
echo "デモ対象プロジェクトのビルド成果物は demo/build に残しています(再実行時に再作成されます)。"

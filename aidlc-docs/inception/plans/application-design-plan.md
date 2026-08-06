# Application Design Plan

## Context
`requirements.md`（FR-1〜FR-8, NFR-1〜NFR-4）に基づき、`ClassScannerRunner`を「抽出層（ClassGraph情報→DTO）」と「書式化層（DTO→CSV/TSV/JSON/YAML）」に分離する設計を行う。User Storiesは省略済みのため、requirements.mdのみを設計インプットとする。

## Plan

- [x] コンポーネント識別・パッケージ構成を決定する
- [x] DTO（レコード型）のフィールド構成を決定する
- [x] 抽出層コンポーネントのメソッドシグネチャを決定する
- [x] 書式化層（Writer）のインターフェース・実装方針を決定する
- [x] サービス層（`ClassScannerRunner`のオーケストレーション方式）を決定する
- [x] コンポーネント間依存関係を決定する
- [x] 上記に基づき、以下の成果物を生成する:
  - [x] `aidlc-docs/inception/application-design/components.md`
  - [x] `aidlc-docs/inception/application-design/component-methods.md`
  - [x] `aidlc-docs/inception/application-design/services.md`
  - [x] `aidlc-docs/inception/application-design/component-dependency.md`
  - [x] `aidlc-docs/inception/application-design/application-design.md`（上記4件の統合ドキュメント）

## Clarifying Questions

以下はAIによる設計提案です。多くの項目は既存コードの慣習（Java 25, Stream API, レコード的な発想）に沿って推奨案を示していますが、特に **Question 3（JSON/YAML時の複数入力集約方式）** は明確な決定が必要な重要ポイントです。

### Question 1: パッケージ構成
新設するDTO層・書式化層をどう配置しますか？

A) サブパッケージで分割する: `cherry.classscanner.model`（DTO）, `cherry.classscanner.extract`（抽出）, `cherry.classscanner.output`（Writer）, `cherry.classscanner`（既存、`Main`+オーケストレーション用の`ClassScannerRunner`のみ残す） ※AI推奨（責務ごとにパッケージを分け、既存の単一パッケージ構成から自然に発展させる）

B) 単一パッケージのまま`cherry.classscanner`にクラスを増やす（現状の構成を維持）

C) その他の構成（[Answer]: の後に具体的に記述してください）

X) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2: DTOの実装方式
`ClassRecord`, `MethodRecord`, `FieldRecord`, `ConstructorRecord`はどう実装しますか？

A) Java 25の`record`型として実装する（不変・簡潔・JacksonのDTOとしても自然に扱える） ※AI推奨（既存コードもJava 25の新機能を積極活用する方針のため）

B) 通常のクラス（getter付き）として実装する

X) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 3: JSON/YAML出力における複数入力ファイルの集約方式
現行のCSV/TSVは、複数のJAR/ディレクトリを引数指定した場合、1ファイルに追記集約されます（初回のみヘッダー出力）。JSON/YAMLは構造化ドキュメントであり、テキスト追記という概念がそのまま適用できません。どう集約しますか？

A) 全入力のレコードをフラットな1つのJSON配列/YAMLシーケンスにまとめる（各レコードの`sourcePath`フィールドで入力元を区別する。CSVと同じ「1レコード1行/1要素」という考え方を保つ） ※AI推奨（CSVとの構造的な一貫性が高く、実装もシンプル）

B) 入力元（ソースパス）ごとにグルーピングした階層構造にする（例: `{ "path/to/app1.jar": [...], "path/to/app2.jar": [...] }`）

C) JSON/YAML出力では複数入力の集約をサポートしない（複数ファイル/ディレクトリ指定時はエラーまたは警告とする）

X) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 4: 書式化層（Writer）のインターフェース設計
CSV/TSV/JSON/YAMLの各Writerをどう抽象化しますか？

A) 共通インターフェース（例: `RecordWriter<T>`）を定義し、CSV用/JSON用/YAML用の実装クラスをそれぞれ用意する（Strategyパターン）。出力種別（classes/methods/fields/constructors）ごとに毎回インスタンス化し、`--format`オプションの値に応じて実装を選択する ※AI推奨（既存の`getCSVFormat`/`getCharset`のようなswitch式による選択ロジックと自然に整合し、新形式追加時の拡張も容易）

B) 単一の汎用Writerクラス内に、フォーマットごとの分岐ロジックをif/switchで持たせる（クラス数を増やさない）

X) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 5: サービス層（オーケストレーション）の責務
`ClassScannerRunner`は最終的にどこまでの責務を持ちますか？

A) `ClassScannerRunner`はCLI引数解析とオーケストレーション（抽出→書式化→出力先決定の呼び出し順序制御）のみを担当し、実際の抽出・書式化ロジックは全て抽出層・書式化層に委譲する ※AI推奨（現状のコード品質評価で指摘した「単一クラスへの機能集中」の解消にもつながる）

B) 現状同様、`ClassScannerRunner`にある程度のロジックを残し、抽出層・書式化層は補助的な利用に留める

X) Other (please describe after [Answer]: tag below)

[Answer]: A

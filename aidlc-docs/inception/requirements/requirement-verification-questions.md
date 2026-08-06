# Requirements Verification Questions

これまでのやり取りは「本プロジェクトの開発プロセスとしてAI-DLCを導入する」というプロセス導入の宣言であり、Reverse Engineering（既存コードの分析）まで完了しています。ただし、これから AI-DLC のプロセスに乗せて実際に何を開発・変更するかはまだ具体的に示されていません。今後の Requirements Analysis 以降を進めるために、以下の質問にお答えください。

## Question 1
AI-DLC導入後、最初にこのプロセスに乗せて取り組みたい開発タスクは何ですか？

A) 既存機能の新規追加・拡張（例: 新しい出力形式やフィルタ条件の追加など）

B) 既知の技術的負債の解消（例: `code-quality-assessment.md` で指摘したテスト未実装の解消など）

C) 特定の不具合修正（詳細は [Answer]: の後に記述してください）

D) まだ具体的なタスクは決まっていない（プロセス導入のみで、今回はここでInception Phaseを一旦終了してよい）

X) Other (please describe after [Answer]: tag below)

[Answer]: A + B（新規追加・拡張、および技術的負債の解消の両方に取り組む。具体的な内容はこの後ユーザーから追って伝えられる）

### Question 1 補足: Aの具体的な内容（ユーザー提示）

1. クラスの一覧もファイル出力できるようにする（現状はmethods/fields/constructorsのみファイル出力可能）。
2. 出力フォーマットとしてJSON, YAMLを追加する（現状はCSV/TSVのみ）。
3. JSON, YAML出力では値が複数ある項目をarrayとして出力する（CSVはこれまで通りデリミタ区切りで1カラムとして出力し、挙動を変えない）。
4. コマンドラインオプション名が `--methods-csv` 等、`*-csv` になっており実態（CSV以外の形式も出力可能）と合わなくなるため、命名を変更したい。

**AIによる命名提案**: `--methods-csv` → `--methods-output`、`--fields-csv` → `--fields-output`、`--constructors-csv` → `--constructors-output`、新規追加する `--classes-output` も同形式に統一。実際の出力形式は既存の `--format=<csv|tsv|json|yaml>` オプションが担うため、オプション名からは形式を切り離す。

## Question 1 Follow-up A: オプション命名規則の選択
上記のAI提案を踏まえ、コマンドラインオプションの命名規則はどれにしますか？

A) `--<種別>-output=<file>` 形式（例: `--methods-output`, `--classes-output`）※AI推奨

B) `--export-<種別>=<file>` 形式（例: `--export-methods`, `--export-classes`）

C) `--<種別>=<file>` 形式（接尾辞なし。例: `--methods`, `--classes`）

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 1 Follow-up B: 旧オプション名の扱い
`--methods-csv` 等の既存オプション名は、リネーム後どう扱いますか？

A) 廃止し、新オプション名のみ提供する（クリーンな置き換え。まだ利用者が限定的な個人/小規模ツールのため） ※AI推奨

B) 非推奨(deprecated)として当面は動作を維持しつつ警告を出す（後方互換性を優先）

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 1 Follow-up C: クラス一覧ファイル出力の項目
新設する `--classes-output` では、クラス1件につきどの情報を出力しますか？

A) 最小限（ソースパス, クラス名）のみ

B) `--verbose` 相当の詳細情報（ソースパス, クラス名, 型[Class/Interface/Abstract/Enum/Annotation], 親クラス, 実装インターフェース, パッケージ）まで含める ※AI推奨（ファイル出力の価値を高めるため）

X) Other (please describe after [Answer]: tag below)

[Answer]: B + 修飾子（public, finalなど） + クラスアノテーション（B相当の項目に加え、クラスの修飾子とクラスレベルのアノテーションも出力する）

### Question 1 Follow-up C 補足: verbose出力への追加要望（ユーザー提示）
`--verbose` のコンソール出力にも、クラスの修飾子（public, finalなど）とクラスアノテーションを追加表示する（現状の`printVerboseClassInfo`はType/Superclass/Interfaces/Packageのみで、修飾子・クラスアノテーションは表示していない）。

## Question 1 Follow-up D: JSON/YAML出力に使うライブラリ
現在の依存関係にはJSON/YAMLシリアライズ用ライブラリが含まれていません（SnakeYAMLはSpring Boot経由の推移的依存として存在しますが、YAML出力を組み立てるための直接依存ではありません）。どのライブラリを新規に追加しますか？

A) Jackson（`jackson-databind` + `jackson-dataformat-yaml`）を追加する ※AI推奨（Spring Bootエコシステムとの親和性が高く、既存のSnakeYAML推移的依存とも整合）

B) その他のライブラリを使う（[Answer]: の後に具体的に記述してください）

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 1 Follow-up E: JSON/YAML出力のキー命名規則
JSON/YAML出力のキー名（フィールド名）は何にしますか？CSVヘッダーは日本語（`ソースパス`, `クラス名` 等）ですが、JSON/YAMLは機械可読性を重視した英語camelCase等にする選択肢もあります。

A) CSVヘッダーと同じ日本語キー名を使う（例: `"クラス名": "..."`）。CSV/JSON/YAML間で項目名の一貫性を保つ

B) 英語camelCaseキー名を使う（例: `"className": "..."`）。一般的なJSON/YAML APIの慣習に合わせる ※AI推奨（JSON/YAMLは主にプログラムからの機械的な消費を想定するフォーマットのため）

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 1 Follow-up F: 技術的負債解消（テスト実装）の進め方
Q1では新機能追加（A）と技術的負債解消（B: テスト未実装の解消）の両方に取り組む方針でしたが、B側の具体的なテスト対象・範囲はまだ示されていません。どう進めますか？

A) 今回のRequirements Analysisでは新機能（A）のみを対象として`requirements.md`を作成し、テスト実装（B）は別の作業単位として後日あらためて要件を出す

B) 今回まとめてテスト実装（B）の詳細もこの場で提示する（[Answer]: の後に対象範囲を記述してください）

X) Other (please describe after [Answer]: tag below)

[Answer]: B（対象範囲は追ってユーザーから提示される）

### Question 1 Follow-up F 補足: テスト範囲とリファクタリング方針（確定）

- **テスト対象範囲**: 基本的に全機能を対象とする（ユーザー指定）。
- **AI提案（合意済み）**:
  - テストフレームワーク: JUnit 5（既存） + AssertJ（既存の`spring-boot-starter-test`経由） + jqwik（PBT用に新規追加）。
  - 対象範囲の切り分け: 既存機能（`Main`, `ClassScannerRunner`の現状の振る舞い全体）をQ1-Bのテスト未整備解消として全面カバーする。Q1-Aで新規追加する機能（classes-output, JSON/YAML出力, camelCaseキー, verboseの修飾子/クラスアノテーション追加）は、実装（Code Generation）と同時にテストも生成する。
  - PBT-10に従いExample-basedテストとPBTをクラス/メソッド名で明確に分離する。
  - PBT候補: ソート順不変条件、パッケージフィルタの不変条件、CSV列構成（先頭列=ソースパス）の不変条件。
- **設計判断（ユーザー合意済み）**: 機能増加（4出力種別 × 複数フォーマット）とテスト容易性の両方の観点から、`ClassScannerRunner`を「抽出（ClassGraph情報→DTO）」と「書式化（DTO→CSV/TSV/JSON/YAML）」に分離するリファクタリングを、Q1-A実装前に行う。これにより要件(3)（JSON/YAMLはarray、CSVはデリミタ結合)をDTO層で自然に実現する。この設計判断はAI-DLCワークフロー上、後続の **Application Design** ステージ（Workflow Planningでの実行判定を経て）で正式に詳細化する。

## Question 2: Security Extensions
Should security extension rules be enforced for this project?

A) Yes — enforce all SECURITY rules as blocking constraints (recommended for production-grade applications)

B) No — skip all SECURITY rules (suitable for PoCs, prototypes, and experimental projects)

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 3: Resiliency Extensions
Should the resiliency baseline be applied to this project?

**What this extension is.** Enabling it applies a set of **directional, design-time best practices** for building resilient systems, derived from the **AWS Well-Architected Framework (Reliability Pillar)** and resilience-review guidance. It steers requirements, design, and code toward fault tolerance, high availability, observability, and recoverability — covering 15 practice areas across business goals, change management, observability, high availability, disaster recovery, and continuous improvement.

**What this extension is NOT.** Enabling it does **not** make your workload production-ready, nor does it certify or guarantee any availability, RTO, or RPO target. It is a **starting point** that scaffolds good resiliency decisions early — it is not a substitute for a formal **AWS Well-Architected Review** of the built system.

A) Yes — apply the resiliency baseline as directional best practices and design-time guidance (recommended for business-critical workloads)

B) No — skip the resiliency baseline (suitable for PoCs, prototypes, and experimental projects; note this CLI tool has no availability/deployment concerns today)

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 4: Property-Based Testing Extension
Should property-based testing (PBT) rules be enforced for this project?

A) Yes — enforce all PBT rules as blocking constraints (recommended for projects with business logic, data transformations, serialization, or stateful components)

B) Partial — enforce PBT rules only for pure functions and serialization round-trips (suitable for projects with limited algorithmic complexity)

C) No — skip all PBT rules (suitable for simple CRUD applications, UI-only projects, or thin integration layers with no significant business logic)

X) Other (please describe after [Answer]: tag below)

[Answer]: B

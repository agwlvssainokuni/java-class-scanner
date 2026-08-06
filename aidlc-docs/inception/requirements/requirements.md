# Requirements

## Intent Analysis Summary

- **User Request**: AI-DLC導入後、最初にこのプロセスに乗せて取り組む開発タスクとして、(A) 出力機能の新規追加・拡張、(B) 技術的負債の解消（テスト未実装の解消）の両方に取り組む。詳細は `aidlc-docs/inception/requirements/requirement-verification-questions.md` のQ1〜Q1 Follow-up A-Fで段階的に確定。
- **Request Type**: Enhancement（既存CLIツールへの機能追加）+ Refactoring（テスト容易性のためのクラス分割）+ Technical Debt Resolution（テスト実装）
- **Scope Estimate**: Multiple Components（`ClassScannerRunner`の抽出/書式化への分離、新規DTO層、新規Writer層、ビルド設定への依存追加）
- **Complexity Estimate**: Moderate（新形式2種の追加とDTO層新設という設計変更を伴うが、外部システム連携や複雑な業務ルールはなく単一モジュール内で完結）

## Functional Requirements

### FR-1: クラス一覧のファイル出力
既存のmethods/fields/constructorsに加え、クラス一覧自体をファイル出力できるようにする。新規オプション `--classes-output=<file>` で有効化する。

出力項目（1クラスにつき1レコード）:
- ソースパス
- クラス名
- 型（Class/Interface/Abstract Class/Enum/Annotation）
- 親クラス（存在する場合）
- 実装インターフェース（複数可）
- パッケージ名
- 修飾子（public, finalなど）
- クラスアノテーション（複数可）

### FR-2: 出力フォーマットへのJSON/YAML追加
既存の `--format=<csv|tsv>` にJSON, YAMLを追加し、`--format=<csv|tsv|json|yaml>` とする。全ての出力（classes/methods/fields/constructors）がJSON/YAMLに対応する。

### FR-3: JSON/YAML出力における複数値項目のarray化
JSON/YAML出力では、値が複数存在する項目（実装インターフェース、各種アノテーション、メソッド/コンストラクタの引数、引数アノテーション等）をネイティブなarrayとして出力する。CSV/TSV出力は現行通りデリミタ区切りの単一カラム文字列とし、挙動を変更しない。

### FR-4: JSON/YAML出力のキー命名規則
JSON/YAML出力のキー名は英語camelCaseとする（例: `sourcePath`, `className`, `methodName`, `returnType`, `parameters`, `modifiers`, `isStatic`, `methodAnnotations`, `parameterAnnotations`）。CSV/TSVのヘッダー（日本語）は現行通り変更しない。

### FR-5: コマンドラインオプションのリネーム
出力先ファイルを指定するオプション名を、実際に扱えるフォーマットがCSV以外にも及ぶことを反映した名称に変更する。

| 旧オプション名 | 新オプション名 |
|---|---|
| `--methods-csv=<file>` | `--methods-output=<file>` |
| `--fields-csv=<file>` | `--fields-output=<file>` |
| `--constructors-csv=<file>` | `--constructors-output=<file>` |
| （新規） | `--classes-output=<file>` |

旧オプション名（`--methods-csv`等）は廃止し、後方互換は提供しない（非推奨期間を設けないクリーンな置き換え）。

### FR-6: verboseコンソール出力への項目追加
`--verbose` 時のコンソール出力（`printVerboseClassInfo`）に、クラスの修飾子（public, finalなど）とクラスアノテーションの表示を追加する。

### FR-7: 既存機能に対するテストの整備
`Main`および`ClassScannerRunner`の現状の振る舞い全体（引数なし/`--quiet`時の挙動、終了コード、CSV/TSV出力のヘッダー・追記制御、`--package`フィルタ、`--charset`/`--format`の不正値フォールバック、`--verbose`出力）を対象に、テストを新規に整備する。FR-1〜FR-6で追加する新機能については、実装（Code Generation）と同時にテストも作成する（別タスクとしては扱わない）。

## Non-Functional Requirements

### NFR-1: 保守性・テスト容易性のためのリファクタリング
出力種別（classes/methods/fields/constructors）4種 × 出力形式（csv/tsv/json/yaml）という組み合わせの増加、およびテスト容易性の両方に対応するため、`ClassScannerRunner`の責務を以下のように分離する。
- **抽出層**: ClassGraphの`ClassInfo`/`MethodInfo`/`FieldInfo`/`ConstructorInfo`から、シンプルなDTO（例: `ClassRecord`, `MethodRecord`, `FieldRecord`, `ConstructorRecord`。複数値項目は`List`として保持）へ変換する。
- **書式化層**: DTOを受け取り、CSV/TSV/JSON/YAMLそれぞれの形式で出力するWriter（例: 共通インターフェースを実装したCSV用/JSON用/YAML用の各Writer）。
- この分離により、FR-3（JSON/YAMLはarray、CSVはデリミタ結合）をDTO層とWriter層の責務分担で自然に実現する。
- 詳細設計は後続の **Application Design** ステージで確定する。

### NFR-2: 依存ライブラリの追加
- JSON/YAML出力のため **Jackson**（`jackson-databind` + `jackson-dataformat-yaml`）を新規依存として追加する。
- テストのため **jqwik**（Property-Based Testing用）を`testImplementation`として新規追加する。JUnit 5・AssertJ（`spring-boot-starter-test`経由）は既存依存を継続利用する。

### NFR-3: 後方互換性
オプション名の変更（FR-5）について、旧オプション名の互換維持は行わない。個人/小規模利用のツールであり、利用者への影響が限定的なため、クリーンな置き換えを優先する。

### NFR-4: テスト・PBT方針
- Example-basedテストとProperty-Based Testを、クラス名/メソッド名で明確に分離する（AI-DLC PBT-10準拠）。
- Partial PBT適用（PBT-02, PBT-03, PBT-07, PBT-08, PBT-09を必須ルールとして適用）。
- PBT候補として以下の不変条件を優先的にテストする:
  - ソート順不変条件（クラス名/メソッド名/フィールド名順、コンストラクタは引数数順）
  - パッケージフィルタの不変条件（フィルタ条件に一致しないクラスが結果に含まれない）
  - CSV/TSV/JSON/YAML間の列・項目構成の不変条件（先頭項目は常にソースパス）
- Round-trip系のPBT-02は、現状は明確な往復変換ペアが存在しないため、JSON/YAML出力実装後に適用対象を再評価する。

## セキュリティ・レジリエンシー拡張の適用可否

- **security-baseline**: 適用しない（ローカル実行のCLIで外部公開・機密データ処理がないため）
- **resiliency-baseline**: 適用しない（可用性・障害復旧の懸念がない単発実行のCLIツールのため）

## Summary（要点）

1. classes-output新設、JSON/YAML形式追加、複数値項目のarray化（CSV挙動は不変）、オプション名リネーム（`*-output`、旧名廃止）、verbose出力への修飾子/クラスアノテーション追加を実装する。
2. 抽出/書式化の責務分離リファクタリングを新機能実装前に行い、後続のApplication Designステージで詳細設計する。
3. Jackson（JSON/YAML用）とjqwik（PBT用）を新規依存として追加する。
4. 既存機能全体を対象にテストを整備し、新機能は実装と同時にテストを作成する。JUnit 5 + AssertJ + jqwik（Partial PBT: PBT-02/03/07/08/09必須）を採用する。
5. security-baseline / resiliency-baseline は適用しない。

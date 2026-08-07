# NFR Design Plan — java-class-scanner

## Context
NFR Requirementsの結果、実質的なNFRは「JSON/YAML集約書き込みの方針（特別対策なし）」と「技術スタック選定（Jackson 3系, jqwik 1.10.1）」のみであった。本ステージでは、これらをコンポーネント設計（Application Design/Functional Design）にどう組み込むかを確定する。

## Plan

- [x] 該当しないNFR設計カテゴリを整理し、根拠を明記する
- [x] Jacksonマッパー（ObjectMapper/YAMLMapper）のインスタンス管理方針を決定する
- [x] JSON出力の整形（pretty-print）方針を決定する
- [x] 上記に基づき、以下の成果物を生成する:
  - [x] `aidlc-docs/construction/java-class-scanner/nfr-design/nfr-design-patterns.md`
  - [x] `aidlc-docs/construction/java-class-scanner/nfr-design/logical-components.md`

## 該当しないNFR設計カテゴリ（AI判定、根拠付き）

- **Resilience Patterns**: N/A — リトライ・フェイルオーバーの概念がなく、エラー時挙動はFunctional DesignのBR-12で確定済み。
- **Scalability Patterns**: N/A — NFR Requirementsで対象外と判定済み。
- **Performance Patterns**: N/A — NFR-R1で「特別な対策を設けない」と決定済み。
- **Security Patterns**: N/A — NFR Requirementsで対象外と判定済み。

## Clarifying Questions

### Question 1: Jacksonマッパー（ObjectMapper/YAMLMapper）のインスタンス管理方針
`JsonRecordWriter`/`YamlRecordWriter`は、それぞれJacksonのマッパーインスタンスをどう管理しますか？

A) クラス内で`static final`な共有インスタンスとして1つ保持し使い回す（Jacksonのマッパーはスレッドセーフかつ生成コストがあるため、共有するのがベストプラクティス）

B) Spring DIで管理する。`JsonRecordWriter`/`YamlRecordWriter`（および`CsvRecordWriter`）を`@Component`とし、コンストラクタインジェクションで`ObjectMapper`（JSON用。Spring Bootの自動構成が提供する既定Beanを利用）・`YAMLMapper`（YAML用。専用の`@Bean`定義を追加）を受け取る。`ClassScannerRunner`も各Writerをコンストラクタインジェクションで受け取り、`--format`値に応じて選択する ※ユーザー指摘により採用（既存の`ClassScannerRunner`自体が`@Component`であり、Spring Bootアプリとして一貫性のあるDIベースの設計とするため。static final共有インスタンスより自然）

X) Other (please describe after [Answer]: tag below)

[Answer]: B

### Question 2: JSON出力の整形（pretty-print）方針
JSON出力はインデント付き（人間が読みやすい整形）にしますか、それともコンパクト（改行・インデントなし）にしますか？

A) インデント付き（pretty-print）で出力する（レビューのしやすさ・可読性を重視。YAMLは元々人間可読な形式のため両形式で見た目の一貫性も保たれる） ※AI推奨

B) コンパクトな1行形式で出力する（ファイルサイズを重視）

X) Other (please describe after [Answer]: tag below)

[Answer]: A

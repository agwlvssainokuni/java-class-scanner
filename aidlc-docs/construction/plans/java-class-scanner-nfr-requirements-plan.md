# NFR Requirements Plan — java-class-scanner

## Context
`execution-plan.md`の判定通り、本ユニットのNFR Requirementsは主に **技術スタック選定**（Jackson, jqwikの具体的な依存関係）が中心であり、性能・セキュリティ・スケーラビリティ面での新規要件はほぼ発生しない（ローカル実行の単発CLIツールのため）。

## Plan

- [ ] 該当しないNFRカテゴリを整理し、根拠を明記する
- [ ] JSON/YAML出力のメモリ使用量に関する方針を決定する
- [ ] Jackson依存関係（groupId/バージョン）を決定する
- [ ] jqwik依存関係（バージョン）を決定する
- [ ] 上記に基づき、以下の成果物を生成する:
  - [ ] `aidlc-docs/construction/java-class-scanner/nfr-requirements/nfr-requirements.md`
  - [ ] `aidlc-docs/construction/java-class-scanner/nfr-requirements/tech-stack-decisions.md`

## 該当しないNFRカテゴリ（AI判定、根拠付き）

- **Scalability Requirements**: N/A — ローカル実行の単発CLIツールであり、同時アクセスや負荷分散の概念が存在しない。
- **Availability Requirements**: N/A — 常駐サービスではなく、稼働率・フェイルオーバーの概念が存在しない。
- **Security Requirements**: N/A（Requirements Analysisで`security-baseline`適用なしと決定済み）。Jacksonは本ツールでは**シリアライズ専用**（JSONの読み込み・逆シリアライズは行わない）であり、デシリアライズ関連の脆弱性面は導入されない。
- **Usability Requirements**: N/A — requirements.mdのFR-1〜FR-6で新規CLIオプション・出力仕様は既に確定済み。追加のUX要件はない。
- **Reliability Requirements**: N/A（新規） — エラー時の挙動はFunctional DesignのBR-12で既に確定済み。

## Clarifying Questions

### Question 1: Jacksonのバージョン系統（groupId）の選定
Spring Boot 4.1.0のBOMは、Jackson 2系（`com.fasterxml.jackson`, レガシー・広く普及）とJackson 3系（`tools.jackson`, Spring Boot 4.1の主要BOMとしてバージョン管理されている新しいパッケージ体系）の両方をインポートしています。どちらを採用しますか？

A) Jackson 3系（`tools.jackson.core:jackson-databind`, `tools.jackson.dataformat:jackson-dataformat-yaml`）を採用し、バージョンはSpring Boot BOM管理に任せる（明示バージョン指定なし） ※AI推奨（本プロジェクトはJava 25・Spring Boot 4.1.0・Gradle 9.6.1と一貫して最新版を採用する方針であり、Spring Boot 4.1のBOMもJackson 3系を主系統として管理しているため）

B) Jackson 2系（`com.fasterxml.jackson.core:jackson-databind`, `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml`）を採用する（広く使われており情報が豊富）

X) Other (please describe after [Answer]: tag below)

[Answer]: 

### Question 2: JSON/YAML集約書き込みのメモリ使用量方針
Functional DesignのBR-9により、JSON/YAML出力は全レコードをメモリ上に蓄積してから1回で書き出します。大規模なJAR/ディレクトリをスキャンした場合、CSV/TSVの逐次書き込みと比べてメモリ使用量が増える可能性があります。この点についてどう扱いますか？

A) 現時点では特別な対策を設けない（本ツールの想定利用規模ではメモリ使用量は問題にならないと判断する。将来的に問題が顕在化した場合に対応する） ※AI推奨

B) メモリ使用量に上限を設ける、またはストリーミングJSON/YAML書き込み（配列を閉じずに逐次書き込む等）を今回の実装に含める

X) Other (please describe after [Answer]: tag below)

[Answer]: 

## Tech Stack Decision（確認）

- **jqwik**: バージョン **1.10.1**（Maven Central最新版、2026-05-29時点）を`testImplementation`として追加する。
- **JUnit 5 / AssertJ**: 既存の`spring-boot-starter-test`経由の依存を継続利用（追加変更なし）。

上記jqwikバージョンでよろしいですか？

[Answer]: 

# NFR Requirements — java-class-scanner

## 該当カテゴリなし（N/A、根拠付き）

| カテゴリ | 判定 | 根拠 |
|---|---|---|
| Scalability | N/A | ローカル実行の単発CLIツールであり、同時アクセスや負荷分散の概念が存在しない |
| Availability | N/A | 常駐サービスではなく、稼働率・フェイルオーバーの概念が存在しない |
| Security | N/A | Requirements Analysisで`security-baseline`適用なしと決定済み。Jacksonはシリアライズ専用（JSON/YAMLの読み込み・逆シリアライズは行わない）であり、デシリアライズ関連の脆弱性面は導入されない |
| Usability | N/A | requirements.mdのFR-1〜FR-6で新規CLIオプション・出力仕様は確定済み。追加のUX要件はない |
| Reliability | N/A（新規なし） | エラー時の挙動はFunctional DesignのBR-12で確定済み |

## 決定事項

### NFR-R1: JSON/YAML集約書き込みのメモリ使用量方針
Functional DesignのBR-9により、JSON/YAML出力は全レコードをメモリ上に蓄積してから1回で書き出す。大規模スキャン時のメモリ使用量について、現時点では特別な対策（上限設定・ストリーミング書き込み等）を設けない。本ツールの想定利用規模ではメモリ使用量は問題にならないと判断する。将来的に問題が顕在化した場合は別途対応する。

### NFR-R2: 技術スタック選定
詳細は`tech-stack-decisions.md`を参照。
- JSON/YAML出力: Jackson 3系（`tools.jackson`）を採用し、バージョンはSpring Boot 4.1.0のBOM管理に委ねる。
- Property-Based Testing: jqwik 1.10.1を`testImplementation`として追加する。

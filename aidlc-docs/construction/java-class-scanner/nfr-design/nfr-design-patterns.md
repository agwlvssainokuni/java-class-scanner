# NFR Design Patterns — java-class-scanner

## 該当しないパターン（N/A、根拠付き）

| カテゴリ | 判定 | 根拠 |
|---|---|---|
| Resilience Patterns | N/A | リトライ・フェイルオーバーの概念がなく、エラー時挙動はFunctional DesignのBR-12で確定済み |
| Scalability Patterns | N/A | NFR Requirementsで対象外と判定済み |
| Performance Patterns | N/A | NFR-R1で「特別な対策を設けない」と決定済み |
| Security Patterns | N/A | NFR Requirementsで対象外と判定済み |

## 採用するパターン

### 依存性注入（DI）によるコンポーネント管理
既存の`ClassScannerRunner`がSpring Bootの`@Component`として実装されていることと一貫性を保つため、新設する`RecordExtractor`および各`RecordWriter`実装（`CsvRecordWriter`, `JsonRecordWriter`, `YamlRecordWriter`）も`@Component`とし、`ClassScannerRunner`へコンストラクタインジェクションで注入する。Jacksonのマッパー（`ObjectMapper`, `YAMLMapper`）についても、`static final`な手動共有ではなくSpring管理のBeanとして注入する（詳細は`logical-components.md`）。

### Strategy パターン（Application Design Q4から継続）
`RecordWriter<T>`共通インターフェース + フォーマットごとの実装というStrategyパターンは維持する。実装インスタンスの生成・管理主体が「手動`new`」から「Spring DI」に変わる点が本ステージでの変更点。

### 出力整形方針
JSON出力はインデント付き（pretty-print）で出力する。YAMLは元来人間可読な形式であり、両形式で見た目の一貫性を保つ。

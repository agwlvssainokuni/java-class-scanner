# Logical Components — java-class-scanner

## Jacksonマッパーの論理コンポーネント

### `ObjectMapper`（JSON用）
- Spring Bootの自動構成（`jackson-databind`がクラスパスに存在する場合に有効化されるJacksonAutoConfiguration相当）が提供する既定のSpring管理Beanを利用する。
- 追加設定として、シリアライズ結果をインデント付き（pretty-print）にする（NFR Design Q2=A）。既定Beanをそのまま使うか、`Jackson2ObjectMapperBuilderCustomizer`相当の仕組みで`SerializationFeature.INDENT_OUTPUT`を有効化するかはCode Generationで確定する。

### `YAMLMapper`（YAML用）
- Spring Bootによる自動構成は提供されないため、専用の設定クラス（例: `@Configuration`クラス内の`@Bean`メソッド）で`YAMLMapper`のBeanを1つ定義する。

## Writerコンポーネント（DIベースに変更、NFR Design Q1=B）

| コンポーネント | 種別 | 注入される依存 |
|---|---|---|
| `CsvRecordWriter` | `@Component` | なし（Apache Commons CSVを直接利用） |
| `JsonRecordWriter` | `@Component` | `ObjectMapper`（コンストラクタインジェクション） |
| `YamlRecordWriter` | `@Component` | `YAMLMapper`（コンストラクタインジェクション） |
| `RecordExtractor` | `@Component` | なし（ClassGraphの戻り値のみを扱う） |
| `ClassScannerRunner` | `@Component`（既存） | `RecordExtractor`, `CsvRecordWriter`, `JsonRecordWriter`, `YamlRecordWriter`（コンストラクタインジェクション） |

`ClassScannerRunner`は、注入された3種類の`RecordWriter`実装の中から`--format`値に応じて使用するインスタンスを選択する（既存の`getCSVFormat`のようなswitch式による選択ロジックを踏襲）。

## コンポーネント管理方針まとめ
- 全ての新設コンポーネントはSpringのデフォルトスコープ（シングルトン）で管理する。
- 手動での`static final`共有インスタンス保持や、呼び出しごとの`new`によるインスタンス生成は行わない。
- これにより、テスト時にはSpringのテストコンテキスト（`@SpringBootTest`）またはモックを用いた依存差し替えが容易になり、`requirements.md` FR-7のテスト整備方針とも整合する。

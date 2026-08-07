# Functional Design Plan — java-class-scanner

## Context
Units Generationは省略済み（単一モジュールのモノリスのため、アプリケーション全体を暗黙の単一ユニット「java-class-scanner」として扱う: `execution-plan.md`参照）。本Functional Designは`aidlc-docs/inception/application-design/`（コンポーネント設計）と`aidlc-docs/inception/requirements/requirements.md`（FR-1〜FR-8, NFR-1〜NFR-4）を踏まえ、詳細なビジネスロジック・ドメインモデル・ビジネスルールを確定する。

## Plan

- [ ] ドメインモデル（DTOのフィールド確定・関係性）を整理する
- [ ] 抽出ロジックのビジネスルール（フィルタリング・ソート順）を確定する
- [ ] 書式化ロジックのビジネスルール（CSV/TSVとJSON/YAMLの差異、null値表現等）を確定する
- [ ] エラーハンドリング（部分失敗時の挙動、不正値フォールバック）を確定する
- [ ] 上記に基づき、以下の成果物を生成する:
  - [ ] `aidlc-docs/construction/java-class-scanner/functional-design/business-logic-model.md`
  - [ ] `aidlc-docs/construction/java-class-scanner/functional-design/business-rules.md`
  - [ ] `aidlc-docs/construction/java-class-scanner/functional-design/domain-entities.md`

## Clarifying Questions

Application Designで確定した設計（DTO/RecordExtractor/RecordWriter、JSON/YAMLの集約書き込み方式）を踏まえ、実装に落とし込む上で確定が必要なビジネスルールを以下に整理しました。AI推奨案を添えています。

### Question 1: JSON/YAML出力中に途中でエラーが発生した場合の挙動
JSON/YAMLは全入力処理後に1回だけ書き出す設計（Application Design Q3=A）です。そのため、複数入力のうち途中の1件でスキャンエラー（`IOException`）が発生した場合、CSV/TSVは既にエラー発生前の分は書き込み済みですが、JSON/YAMLはまだ何も書き込まれていません。この場合どうしますか？

A) それまでに正常処理できた分だけを集約してJSON/YAMLファイルに書き出す（ベストエフォート。CSV/TSVの「エラー前の分は残る」という挙動になるべく近づける） ※AI推奨

B) 何も書き出さない（全件成功した場合のみJSON/YAMLファイルを生成する。一部でも失敗したら出力ファイルなし）

X) Other (please describe after [Answer]: tag below)

[Answer]: 

### Question 2: 抽出結果が0件の場合の出力ファイル生成
対象クラスが0件、またはパッケージフィルタで全て除外された場合、出力ファイルはどう扱いますか？

A) 空配列/空シーケンス（JSON: `[]`、YAML: `[]`）としてファイルは生成する（CSVが常にヘッダー行を書き込む既存挙動と一貫性を保つ） ※AI推奨

B) 該当するデータが1件もない場合はファイル自体を生成しない

X) Other (please describe after [Answer]: tag below)

[Answer]: 

### Question 3: null値フィールドのJSON/YAML表現
`ClassRecord.superclass`のように値が存在しない場合がある項目（例: インターフェースやObjectクラス自身には親クラスがない）を、JSON/YAMLでどう表現しますか？

A) 明示的に`null`を出力する（例: `"superclass": null`）。全レコードで同一のキー構成が保たれ、機械的なパース・スキーマ検証がしやすい ※AI推奨

B) 値が存在しない場合はキー自体を省略する（Jacksonの`@JsonInclude(NON_NULL)`相当）。出力サイズはやや小さくなるが、レコードごとにキー構成が変わりうる

X) Other (please describe after [Answer]: tag below)

[Answer]: 

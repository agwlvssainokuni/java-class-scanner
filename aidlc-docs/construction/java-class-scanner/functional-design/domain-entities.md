# Domain Entities — java-class-scanner

Application Designで定義したDTO（`components.md`/`component-methods.md`）を、ビジネス上の意味を持つドメインエンティティとして詳細化する。実装言語（Java 25 record等）には依存しない概念モデルとして記述する。

## エンティティ一覧

### ClassRecord（クラス情報）
1件のJavaクラス/インターフェース/列挙型/アノテーション型を表す。

| 属性 | 型 | 必須 | 説明 |
|---|---|---|---|
| sourcePath | 文字列 | 必須 | 入力元（JAR/ディレクトリ）のパス |
| className | 文字列 | 必須 | 完全修飾クラス名 |
| type | 列挙（Class/Interface/Abstract Class/Enum/Annotation） | 必須 | クラスの種別 |
| superclass | 文字列 | **任意（値なしの場合あり）** | 親クラス名。インターフェース、`Object`自身等は値を持たない |
| interfaces | 文字列の集合（順序保持） | 必須（0件も可） | 実装インターフェース名 |
| packageName | 文字列 | 必須 | パッケージ名 |
| modifiers | 文字列 | 必須 | 修飾子（`public final`等、単一文字列として保持。詳細は`business-rules.md`） |
| classAnnotations | 文字列の集合（順序保持） | 必須（0件も可） | クラスアノテーション名 |

### MethodRecord（メソッド情報）
`isRegularMethod`判定を満たす1件のメソッドを表す（コンストラクタ・静的初期化子・ラムダ実装メソッドは対象外。詳細は`business-rules.md`）。

| 属性 | 型 | 必須 | 説明 |
|---|---|---|---|
| sourcePath | 文字列 | 必須 | |
| className | 文字列 | 必須 | |
| methodName | 文字列 | 必須 | |
| returnType | 文字列 | 必須 | |
| parameters | 文字列の順序付きリスト | 必須（0件も可） | 引数の型シグネチャ |
| modifiers | 文字列 | 必須 | |
| isStatic | 真偽値 | 必須 | |
| methodAnnotations | 文字列の集合 | 必須（0件も可） | |
| parameterAnnotations | 「引数ごとのアノテーション集合」の順序付きリスト | 必須（0件も可、または各要素が0件も可） | `parameters`と同じ並び順・同じ要素数を持つ |

### FieldRecord（フィールド情報）

| 属性 | 型 | 必須 | 説明 |
|---|---|---|---|
| sourcePath | 文字列 | 必須 | |
| className | 文字列 | 必須 | |
| fieldName | 文字列 | 必須 | |
| fieldType | 文字列 | 必須 | |
| modifiers | 文字列 | 必須 | |
| isStatic | 真偽値 | 必須 | |
| fieldAnnotations | 文字列の集合 | 必須（0件も可） | |

### ConstructorRecord（コンストラクタ情報）

| 属性 | 型 | 必須 | 説明 |
|---|---|---|---|
| sourcePath | 文字列 | 必須 | |
| className | 文字列 | 必須 | |
| parameters | 文字列の順序付きリスト | 必須（0件も可） | |
| modifiers | 文字列 | 必須 | |
| constructorAnnotations | 文字列の集合 | 必須（0件も可） | |
| parameterAnnotations | 「引数ごとのアノテーション集合」の順序付きリスト | 必須（0件も可） | |

## エンティティ関係
- 1つの`ClassRecord`に対し、0件以上の`MethodRecord`・`FieldRecord`・`ConstructorRecord`が`className`（+ `sourcePath`）を通じて論理的に関連付く（DTOレベルでは相互参照を持たない独立したフラットなレコード群として設計する。関連付けが必要な消費者は`className`と`sourcePath`の組で突き合わせる）。
- `sourcePath`は全エンティティに共通する識別属性であり、複数入力ファイル/ディレクトリを横断した集約時の区別に用いる（承認済み: Application Design Q3）。

## 値なし（任意）属性の表現ルール
`superclass`のように値を持たない場合があるフィールドは、出力（JSON/YAML）において**明示的な値なし（null相当）として表現し、キー自体は省略しない**（承認済み: Functional Design Q3=A）。これにより、全レコードが同一のキー構成を持ち、機械的なパース・スキーマ検証が容易になる。CSV/TSVでは従来通り空文字列として表現する（既存挙動を変更しない）。

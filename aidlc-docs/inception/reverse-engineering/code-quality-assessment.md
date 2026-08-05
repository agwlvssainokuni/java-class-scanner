# Code Quality Assessment

## Test Coverage

- **Overall**: None（テストコードが存在しない）
- **Unit Tests**: 未実装。`src/test/java` ディレクトリが存在しない。
- **Integration Tests**: 未実装。
- **備考**: `build.gradle` は `spring-boot-starter-test` への依存と `Test` タスクの `useJUnitPlatform()` を設定済みであり、テスト基盤自体は導入されている。実行しても `NO-SOURCE` としてスキップされる状態。

## Code Quality Indicators

- **Linting**: 専用のLintツール（Checkstyle/SpotBugs等）の設定は見当たらない。IntelliJ IDEA の `.idea/` 設定（`compiler.xml` の `-parameters` オプション等）でコンパイラ設定を管理。
- **Code Style**: 概ね一貫している。Stream API・メソッド参照・`var`・Java 25の `getFirst()` やswitch式などモダンな記法を統一的に使用。ヘルパーメソッド抽出によるDRY原則の適用も一貫。
- **Documentation**: Fair〜Good。各ソースファイルにApache License 2.0のヘッダーコメントあり。`README.md`/`README_en.md`（利用者向け）と `CLAUDE.local.md`（開発者/AI向け）が整備されており、コミット履歴（例: `依存ライブラリを最新版へアップデートし、deprecated APIを修正。`）からも継続的なメンテナンスが確認できる。ただしJavadocコメントはソースコード中にほぼ無い。

## Technical Debt

- **テスト不在**: `ClassScannerRunner` の引数解析・CSV出力・フィルタリングロジックに対する自動テストがなく、リグレッション検知ができない状態。CSVヘッダー管理（`csvFilesCreated`）やcharset/formatのフォールバック処理など分岐が多く、テストの価値が高い箇所。
- **単一クラスへの機能集中**: `ClassScannerRunner`（約500行）に引数解析・スキャン・出力（3種類）・整形ヘルパーが全て集約されており、今後機能追加する場合は責務分割（例: CSV出力担当クラスの抽出）を検討する余地がある。
- **Lintツール未導入**: 静的解析ツールによる自動チェックがなく、コードスタイルは目視レビューに依存している。

## Patterns and Anti-patterns

- **Good Patterns**:
  - Spring Boot `ApplicationRunner`/`ExitCodeGenerator` を用いたCLIの標準的な実装パターン。
  - 文字列変換ロジック（`parametersToString`等）のヘルパーメソッド抽出によるDRY。
  - `try-with-resources` によるリソース管理（`ScanResult`, `FileWriter`, `CSVPrinter`）。
  - `@Nonnull`/`@Nullable` によるnull安全性の明示。
  - 全出力の一貫したソート（クラス名/メソッド名/フィールド名/コンストラクタ引数数順）。
- **Anti-patterns**:
  - 特に目立った深刻なアンチパターンは検出されず。強いて挙げれば、`ClassScannerRunner` が単一責任の観点でやや肥大化している点（上記Technical Debt参照）。

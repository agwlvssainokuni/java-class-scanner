# Component Inventory

## Application Packages

- `cherry.classscanner` - CLIアプリケーション本体（`Main`, `ClassScannerRunner`）。単一パッケージ構成。

## Infrastructure Packages

なし（CDK/Terraform等のインフラ定義は本リポジトリに含まれない）

## Shared Packages

なし（共有Models/Utilities/Clientsパッケージへの分割はされておらず、`cherry.classscanner` 単一パッケージに集約）

## Test Packages

なし（`src/test` にテストコードは未実装。`build.gradle` は `useJUnitPlatform()` を設定済みでテスト基盤は準備されている）

## Total Count

- **Total Packages**: 1
- **Application**: 1
- **Infrastructure**: 0
- **Shared**: 0
- **Test**: 0

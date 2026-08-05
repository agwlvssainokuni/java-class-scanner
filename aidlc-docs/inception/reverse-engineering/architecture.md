# System Architecture

## System Overview

java-class-scanner は単一モジュールのSpring Boot製コマンドラインアプリケーションである。Webサーバー機能は使用せず、`ApplicationRunner` を実装したコンポーネントがCLIとしての起動〜終了までの処理を担う。外部システムやデータベースへの接続はなく、入力はローカルファイルシステム上のJAR/クラスファイル、出力はコンソールログとローカルCSV/TSVファイルに限定される。

## Architecture Diagram

```mermaid
flowchart TD
    subgraph App["java-class-scanner (単一Gradleモジュール)"]
        Main["Main<br/>(@SpringBootApplication)"]
        Runner["ClassScannerRunner<br/>(@Component, ApplicationRunner, ExitCodeGenerator)"]
        Props["application.properties<br/>(バナー無効化・ログレベル設定)"]
    end

    subgraph External["外部ライブラリ"]
        ClassGraph["ClassGraph<br/>(クラスファイル静的解析)"]
        CommonsCSV["Apache Commons CSV<br/>(CSV/TSV書き込み)"]
        CommonsLang3["Apache Commons Lang3<br/>(文字列ユーティリティ)"]
        SpringBoot["Spring Boot Framework<br/>(DI・起動制御)"]
    end

    FS["ローカルファイルシステム<br/>(JAR / ディレクトリ / 出力CSV)"]

    Main -->|"SpringApplication.run"| SpringBoot
    SpringBoot -->|"DI"| Runner
    Runner -->|"scan()"| ClassGraph
    Runner -->|"CSVPrinter"| CommonsCSV
    Runner -->|"trim等"| CommonsLang3
    Runner -->|"読み込み"| FS
    Runner -->|"書き込み"| FS
    Props -.->|"設定"| SpringBoot
```

## Component Descriptions

### Main
- **Purpose**: アプリケーションのエントリポイント。
- **Responsibilities**: Spring Bootコンテキストの起動と終了、プロセス終了コードのOSへの返却。
- **Dependencies**: `spring-boot-starter`（`SpringApplication`）。
- **Type**: Application

### ClassScannerRunner
- **Purpose**: コマンドライン引数を解釈し、ClassGraphでスキャンした結果をコンソール/CSV出力する。
- **Responsibilities**: 引数解析、ファイル存在チェック、ClassGraphスキャン実行、パッケージフィルタリング、コンソール出力（標準/詳細/quiet）、CSV/TSV出力（メソッド/フィールド/コンストラクタ、ヘッダー管理付き追記）、文字コード/フォーマット解決、終了コード保持。
- **Dependencies**: `classgraph`（スキャン）、`commons-csv`（CSV出力）、`commons-lang3`（`StringUtils.trim`）、`spring-boot-starter`（`ApplicationRunner`/`ExitCodeGenerator`/`ApplicationArguments`）、`slf4j`（ロギング）。
- **Type**: Application

## Data Flow

```mermaid
sequenceDiagram
    participant User as 実行者(CLI)
    participant Main as Main
    participant Boot as SpringApplication
    participant Runner as ClassScannerRunner
    participant CG as ClassGraph
    participant FS as ファイルシステム

    User->>Main: java -jar app.jar [options] <file|dir>...
    Main->>Boot: SpringApplication.run(Main.class, args)
    Boot->>Runner: run(ApplicationArguments)
    alt 非オプション引数なし
        Runner-->>User: 使用方法をログ出力 (exitCode=0)
    else 引数あり
        Runner->>Runner: findProcessableFiles() で存在確認
        loop 各ファイル/ディレクトリ
            Runner->>CG: overrideClasspath(path).scan()
            CG-->>Runner: ScanResult(全クラス)
            Runner->>Runner: パッケージフィルタ・ソート適用
            opt --methods-csv / --fields-csv / --constructors-csv 指定
                Runner->>FS: CSVPrinterで書き込み(初回ヘッダー/以降追記)
            end
            opt --quiet 未指定
                Runner-->>User: クラス一覧をログ出力(標準/詳細)
            end
        end
        Runner-->>Boot: exitCode (0=成功, 1=IOException発生)
    end
    Boot-->>Main: SpringApplication.exit(context)
    Main-->>User: System.exit(exitCode)
```

## Integration Points

- **External APIs**: なし（外部ネットワーク通信を行わない）
- **Databases**: なし
- **Third-party Services**: なし（ライブラリ依存のみ。詳細は `dependencies.md` を参照）

## Infrastructure Components

- **CDK Stacks**: なし
- **Deployment Model**: `./gradlew bootJar` で生成した実行可能JAR (`java -jar java-class-scanner.jar`) をローカル/CI環境で直接実行する単純な配布モデル。専用のデプロイ先インフラは持たない。
- **Networking**: なし（ネットワーク通信不要のスタンドアロンCLI）

# Requirements Verification Questions

これまでのやり取りは「本プロジェクトの開発プロセスとしてAI-DLCを導入する」というプロセス導入の宣言であり、Reverse Engineering（既存コードの分析）まで完了しています。ただし、これから AI-DLC のプロセスに乗せて実際に何を開発・変更するかはまだ具体的に示されていません。今後の Requirements Analysis 以降を進めるために、以下の質問にお答えください。

## Question 1
AI-DLC導入後、最初にこのプロセスに乗せて取り組みたい開発タスクは何ですか？

A) 既存機能の新規追加・拡張（例: 新しい出力形式やフィルタ条件の追加など）

B) 既知の技術的負債の解消（例: `code-quality-assessment.md` で指摘したテスト未実装の解消など）

C) 特定の不具合修正（詳細は [Answer]: の後に記述してください）

D) まだ具体的なタスクは決まっていない（プロセス導入のみで、今回はここでInception Phaseを一旦終了してよい）

X) Other (please describe after [Answer]: tag below)

[Answer]: A + B（新規追加・拡張、および技術的負債の解消の両方に取り組む。具体的な内容はこの後ユーザーから追って伝えられる）

## Question 2: Security Extensions
Should security extension rules be enforced for this project?

A) Yes — enforce all SECURITY rules as blocking constraints (recommended for production-grade applications)

B) No — skip all SECURITY rules (suitable for PoCs, prototypes, and experimental projects)

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 3: Resiliency Extensions
Should the resiliency baseline be applied to this project?

**What this extension is.** Enabling it applies a set of **directional, design-time best practices** for building resilient systems, derived from the **AWS Well-Architected Framework (Reliability Pillar)** and resilience-review guidance. It steers requirements, design, and code toward fault tolerance, high availability, observability, and recoverability — covering 15 practice areas across business goals, change management, observability, high availability, disaster recovery, and continuous improvement.

**What this extension is NOT.** Enabling it does **not** make your workload production-ready, nor does it certify or guarantee any availability, RTO, or RPO target. It is a **starting point** that scaffolds good resiliency decisions early — it is not a substitute for a formal **AWS Well-Architected Review** of the built system.

A) Yes — apply the resiliency baseline as directional best practices and design-time guidance (recommended for business-critical workloads)

B) No — skip the resiliency baseline (suitable for PoCs, prototypes, and experimental projects; note this CLI tool has no availability/deployment concerns today)

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 4: Property-Based Testing Extension
Should property-based testing (PBT) rules be enforced for this project?

A) Yes — enforce all PBT rules as blocking constraints (recommended for projects with business logic, data transformations, serialization, or stateful components)

B) Partial — enforce PBT rules only for pure functions and serialization round-trips (suitable for projects with limited algorithmic complexity)

C) No — skip all PBT rules (suitable for simple CRUD applications, UI-only projects, or thin integration layers with no significant business logic)

X) Other (please describe after [Answer]: tag below)

[Answer]: B

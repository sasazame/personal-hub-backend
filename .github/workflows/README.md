# GitHub Actions CI/CD Pipeline

## 概要
このディレクトリには、TODO App BackendのCI/CDパイプライン設定が含まれています。

## ワークフロー

### CI Pipeline (`ci.yml`)
Pull RequestとmainブランチへのプッシュでトリガーされるCIパイプライン。

#### ジョブ構成

1. **test** - テスト実行
   - PostgreSQLサービスコンテナを起動
   - 単体テストを実行
   - 統合テストを実行
   - テストレポートを生成
   - カバレッジレポートを生成・アップロード

2. **build** - ビルド検証
   - アプリケーションをビルド
   - JARファイルを成果物として保存

3. **security-scan** - セキュリティスキャン
   - OWASP Dependency Checkを実行
   - 脆弱性レポートを生成

4. **code-quality** - コード品質チェック
   - SpotBugsで静的解析
   - PMDでコード品質チェック

## 必要な設定

### リポジトリシークレット
以下のシークレットを設定することで、追加機能が有効になります：
- `CODECOV_TOKEN`: Codecovへのカバレッジアップロード用（オプション）

### ブランチ保護ルール
mainブランチに以下の保護ルールを設定することを推奨：
- Pull Request必須
- ステータスチェック必須（test, build）
- ブランチを最新に保つ

## ローカルでのテスト実行

```bash
# 単体テストのみ
mvn clean test

# 統合テスト含む全テスト
mvn clean verify

# カバレッジレポート生成
mvn clean test jacoco:report

# コード品質チェック
mvn spotbugs:check
mvn pmd:check
```

## トラブルシューティング

### PostgreSQL接続エラー
CI環境では、PostgreSQLサービスコンテナが自動的に起動されます。
ローカルでテストする場合は、PostgreSQLが起動していることを確認してください。

### カバレッジ閾値エラー
現在、パッケージレベルで80%のカバレッジが必要です。
閾値は`pom.xml`のJaCoCoプラグイン設定で変更できます。

## 関連ドキュメント
- [Maven Surefire Plugin](https://maven.apache.org/surefire/maven-surefire-plugin/)
- [Maven Failsafe Plugin](https://maven.apache.org/surefire/maven-failsafe-plugin/)
- [JaCoCo](https://www.jacoco.org/jacoco/)
- [SpotBugs](https://spotbugs.github.io/)
- [PMD](https://pmd.github.io/)
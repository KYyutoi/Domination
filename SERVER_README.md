# Domination_test Minecraft Server

## エクスプローラーからサーバーを起動する方法

### 方法1: シンプルに起動 ⚡
プロジェクトフォルダで **`StartServer.bat`** をダブルクリック
- 最もシンプルな起動方法
- 自動的にサーバーをビルドして起動

### 方法2: メニュー付きランチャー 📋
プロジェクトフォルダで **`ServerLauncher.bat`** をダブルクリック
- メニューから以下の操作が可能：
  1. サーバー起動
  2. サーバー停止
  3. プラグインのみビルド
  4. クリーンビルド
  5. ログ表示
  6. 終了

### 方法3: PowerShellスクリプト 🔧
プロジェクトフォルダで **`StartServer.ps1`** を右クリック → 「PowerShellで実行」
- より詳細な起動情報を表示
- ロックファイルの自動クリーンアップ機能付き

### デスクトップショートカットを作成
**`CreateDesktopShortcut.ps1`** を右クリック → 「PowerShellで実行」
- デスクトップにショートカットが作成されます
- 次回からデスクトップから起動可能

## サーバーを停止する方法
- サーバーウィンドウで **Ctrl+C** を押す
- または **`StopServer.bat`** をダブルクリック

## 接続方法
1. Minecraftクライアント (バージョン1.21) を起動
2. マルチプレイヤー → サーバーを追加
3. サーバーアドレス: **`localhost`**
4. 接続！

## トラブルシューティング

### 「ファイルがロックされています」エラー
- **`StopServer.bat`** を実行してから再起動
- または **`StartServer.ps1`** を使用（自動でロックを解除）

### ポート25565が使用中
- 既に他のMinecraftサーバーが起動していないか確認
- `run\server.properties` でポート番号を変更可能

### プラグインが反映されない
- **`ServerLauncher.bat`** → [4] Clean Build を実行
- サーバーを再起動

## ファイル説明
- **StartServer.bat** - シンプルなサーバー起動スクリプト
- **StartServer.ps1** - 高機能なPowerShell起動スクリプト
- **StopServer.bat** - サーバー停止スクリプト
- **ServerLauncher.bat** - メニュー付きランチャー
- **CreateDesktopShortcut.ps1** - デスクトップショートカット作成

## 開発時のヒント
- プラグインコードを変更したら、サーバーを再起動して反映
- ログは `run\logs\latest.log` で確認
- プラグイン本体は `src\main\java` に配置


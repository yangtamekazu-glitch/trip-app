🌍 DailyTrips - お出かけスポット探索アプリ
DailyTrips は、現在地周辺の魅力的なスポットを直感的に発見できる、Android向けのお出かけ支援アプリです。Google Maps SDKとGoogle Places APIを活用し、写真を中心とした美しいグリッドレイアウトで「次の目的地」を提案します。

✨ 主な機能
📍 リアルタイム周辺検索: 現在地から最大10km圏内のスポットを瞬時に検索。

⭐ インテリジェントな「おすすめ」: 評価（星）が4.0以上の高評価スポットを優先的に表示。

🛠️ カテゴリーの自由なカスタマイズ:

ユーザー自身でカテゴリーを追加・削除。

カテゴリーの表示順を並び替え可能。

📏 検索範囲の調整: 1kmから10kmまで、スライダーで検索半径を自由に変更。

🔍 キーワード検索: カテゴリーにない特定の場所も、検索バーから自由自在に探索。

🗺️ マップ連携: 気に入ったスポットをタップすると、Googleマップアプリが起動し、即座にルート検索が可能。

🛠 技術スタック
言語: Kotlin

UIフレームワーク: Jetpack Compose (Material Design 3)

地図: Google Maps Compose Library

ネットワーク: Retrofit / OkHttp

画像処理: Coil (非同期画像読み込み)

アーキテクチャ: MVVM (ViewModel, StateFlow)

位置情報: Google Play Services Location

🚀 セットアップ方法
1. APIキーの取得
Google Cloud Console にアクセスします。

Maps SDK for Android と Places API (New) を有効にします。

APIキーを作成します。

2. 環境変数の設定
プロジェクトの local.properties ファイルに取得したAPIキーを追加します：

Properties
MAPS_API_KEY=あなたのAPIキーをここに貼り付け
3. ビルドと実行
Android Studioでプロジェクトを開き、gradlew assembleRelease または実行ボタン（▶️）を押して実機・エミュレータで起動します。

📸 スクリーンショットの構成
アプリは上下2層構造になっています：

上部（Map）: 現在地と周辺スポットのピンを表示。

下部（Feed）: スポットの写真をピンタレスト風のグリッドで表示。評価と名前をオーバーレイ。

📝 ライセンス
このプロジェクトは学習目的で作成されました。

💡 補足説明（開発者向け）
このREADME.mdは、誰が見ても「どんなアプリか」が30秒で理解できるように構成しています。

機能一覧: 開発した「星4以上」「カテゴリー編集」などの特徴を強調しています。

技術スタック: 実装に使用したライブラリを明記し、技術力の証明になります。

セットアップ: local.properties を使う手法はセキュリティ上の定石（APIキーをGitHubに上げないため）を反映しています。



サービスURL
https://trip-app-2qyu.onrender.com/

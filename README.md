# KS Measuring

KS Measuring は、スマートフォンのモバイル通信状態（LTE / 5G など）をリアルタイムで表示・記録できる Android アプリです。

## 主な機能

- RSRP / SINR / RSRQ などの詳細な無線通信情報の表示
- 通信速度（ダウンロード / アップロード）の表示
- ネイバーセル（周辺セル）の情報表示
- CSV形式でログ記録（`/Documents/ksmeasuring` に保存）
- RSRPヒストリのグラフ表示

## スクリーンショット

| 無線指標表示画面1 | 無線指標表示画面2 |
|--------------|------------|
| ![無線指標表示画面1](./KS%20Measuring3.jpg) | ![無線指標表示画面2](./KS%20Measuring2.jpg) |

## ダウンロード

- [最新のAPKをダウンロード](https://github.com/kayamasoft/KSMeasuring/releases/latest)
- またはソースコードからビルドしてください

## ビルド方法（Android Studio）

1. リポジトリをクローン：
   ```
   git clone https://github.com/kayamasoft/KSMeasuring.git
   cd KSMeasuring.git
   ```

2. Android Studio で開く（APIレベル29以上が必要）

3. メニューから「Build > Generate Signed APK」を実行

## 使用する権限

- ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION（セル情報取得）
- READ_PHONE_STATE（通信状態取得）
- INTERNET, ACCESS_NETWORK_STATE（ネットワーク状態確認）
- WRITE_EXTERNAL_STORAGE（CSVログ保存）

## プライバシーポリシー

https://www.kayamasoft.org/privacy.html

## お問い合わせ先

- Webサイト: https://www.kayamasoft.org
- メール: hello@kayamasoft.org

© 2025 KayamaSoft. All rights reserved.

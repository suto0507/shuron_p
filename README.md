# 修論のプログラム

- このツールを動作させるためには、z3がダウンロードされていて、パスが通っていることが必要です。

### サンプルプログラム

- [サンプルプログラム](https://github.com/suto0507/shuron_testcase)

### オプション一覧
- 篩型の制約を追加する深度
    - 篩型の述語にフィールドが含まれていることでループが起こることを防ぐため、篩型の制約を追加する深度に制限を設けている。
    - デフォルト値は10

```
-refinement_type_limmit <unsigned int value>
```
- 変数を探索する深度
    - 検証を行う際に、全ての変数が持つクラスの不変条件や篩型、参照など、全ての変数について検証を行わなければいけない場合がある。
しかし、全ての変数を列挙することはできない場合がある。そのため、本研究のツールでは、変数を探索する深度に制限を設けている。
    - デフォルト値は10
```
-field_limmit <unsigned int value>
```
- 時間制限
    - z3による検証を行う際の時間制限(msec)
    - デフォルト値の場合、時間制限はなし
```
-timeout <unsigned int value>
```

### コマンドの実行例
- この例では、A.javaとB_valid.javaに関して検証を行う()

- また、オプションとして篩型の制約を追加する深度を設定している。
```
java -jar s_jml.jar C:\Users\user\Documents\A.java C:\Users\user\Documents\B_valid.java -refinement_type_limmit 20
```
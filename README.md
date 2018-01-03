# java-packaging-samples-java8-201801

Javaアプリケーションのパッケージング(jar, warなど)について2018年1月時点で調査したメモと、サンプルコードです。

サンプルコードのコンパイル・ビルド・動作環境:
- OS : Windows10 Pro 64bit日本語版
- Oracle JDK 1.8.0_131
- Maven : 3.3.9 以上

# jarファイルの作成と実行

- jarファイルとは : 複数のclassファイルをパッケージングしたもの。zipファイルのフォーマットと互換性があり、拡張子を.zipにすればそのままzipファイルとして展開できる。
  - https://docs.oracle.com/javase/jp/8/docs/technotes/guides/jar/index.html

```
cd 01_simple-jar/
javac -encoding UTF-8 samplepkg/HelloWorld.java
jar cvf hello-world.jar samplepkg/HelloWorld.class
java -cp hello-world.jar samplepkg.HelloWorld
```

# 実行可能なjarファイル

MANIFEST.MF の `Main-Class` で実行対象のクラス名を設定する。

```
cd 02_simple-executable-jar/
javac -encoding UTF-8 samplepkg/HelloWorld.java
jar cvfm hello-world.jar MANIFEST.MF samplepkg/HelloWorld.class
java -jar hello-world.jar
```

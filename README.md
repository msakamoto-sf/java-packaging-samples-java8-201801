# java-packaging-samples-java8-201801

Javaアプリケーションのパッケージング(jar, warなど)について2018年1月時点で調査したメモと、サンプルコードです。

サンプルコードのコンパイル・ビルド・動作環境:
- OS : Windows10 Pro 64bit日本語版
- Oracle JDK 1.8.0_131
- Maven : 3.3.9 以上

## 01. jarファイルの作成と実行

- jarファイルとは : 複数のclassファイルをパッケージングしたもの。zipファイルのフォーマットと互換性があり、拡張子を.zipにすればそのままzipファイルとして展開できる。
  - https://docs.oracle.com/javase/jp/8/docs/technotes/guides/jar/index.html

```
cd 01_simple-jar/
javac -encoding UTF-8 samplepkg/HelloWorld.java
jar cvf hello-world.jar samplepkg/HelloWorld.class
java -cp hello-world.jar samplepkg.HelloWorld
```

## 02. 実行可能なjarファイル

MANIFEST.MF の `Main-Class` で実行対象のクラス名を設定する。
- https://docs.oracle.com/javase/jp/8/docs/technotes/guides/jar/jar.html#Main_Attributes

```
cd 02_simple-executable-jar/
javac -encoding UTF-8 samplepkg/HelloWorld.java
jar cvfm hello-world.jar MANIFEST.MF samplepkg/HelloWorld.class
java -jar hello-world.jar
```

## 03. Maven Shade Pluginによる uber-jar の作成

- "uber" というのはドイツ語の Über から来ており、"over"/"above"という意味らしい。
  - via : https://stackoverflow.com/questions/11947037/what-is-an-uber-jar
- "uber-jar" は "fat jar" あるいは "jar with dependencies" とも呼ばれており、要するに「依存関係のライブラリの中身を全部取り込んだ、全部入りjar」で、これにより依存関係にあるjarライブラリをパッケージングしたりクラスパスに指定する問題を解決する。
- "jar with dependencies" は [Maven Assembly Plugin](http://maven.apache.org/plugins/maven-assembly-plugin/) を使って作ることができる。
  - http://maven.apache.org/plugins/maven-assembly-plugin/descriptor-refs.html#jar-with-dependencies
- さらに細かい調整をしてくれる [Maven Shade Plugin](http://maven.apache.org/plugins/maven-shade-plugin/) を使うこともできて、MANIFEST.MFの設定も同時にできる。
  - http://maven.apache.org/plugins/maven-shade-plugin/
- つまり Maven Shade Plugin を使えば、依存関係全部入りの、ダブルクリックで単体起動できるjarファイルを作成できる。

```
cd 03_shaded-executable-uber-jar/
./mvnw package
java -jar target/hello-maven-1.0-SNAPSHOT.jar
```

## その他の参考資料

- One-JARでアプリケーションの配布を単純化
  - https://www.ibm.com/developerworks/jp/java/library/j-onejar/index.html
  - 依存するjarファイルを、jarのままアプリケーションjarの中に埋め込む。
  - クラスパスの解決策として、`onejar:` というプロトコルプレフィックスでjar内のjarを探すカスタムクラスローダを実装して解決している。
  - アプローチとしては Spring Boot で生成されるjarファイルの構成に近い。
- Packing your Java application as one (or fat) JAR
  - https://www.javacodegeeks.com/2012/11/packing-your-java-application-as-one-or-fat-jar.html
  - [onejar-maven-plugin](https://code.google.com/archive/p/onejar-maven-plugin/) を使った例。
- Fat Jar Eclipse Plug-In
  - http://fjep.sourceforge.net/
  - Eclipse から fat-jar を生成するプラグイン
- 参考メモ/Java Servletアプリで実行可能なwarの作り方の参考URLメモ - Qiita
  - https://qiita.com/msakamoto_sf/items/157c0266544ac012be89




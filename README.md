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

## 04. 署名済みjarファイルとuber-jarの相性問題

Maven Shade Plugin を使うと簡単にuber-jarを作成できるが、相性の悪いjarファイルというのがある。
その例として、署名済みjarファイルがある。
- 署名済みjarファイルにはMETA-INF中に署名に使う鍵や署名データがが含まれている。
- uber-jarを生成によりMETA-INFにもそれらのデータが引き継がれるが、アプリケーションのクラスやその他の依存ライブラリもjarに含まれてしまい、生成されたuber-jarの署名検証に失敗してしまう。
- 結果として生成されたjarファイルを実行できないことになる。

サンプルコード `04_shaded-uber-with-signed-jar` では署名済みjarファイルのライブラリとして [Bouncy Castle](https://www.bouncycastle.org/java.html) を組み込んでみた。
上記問題を解決するため、pom.xml の以下の設定により署名データを生成したuber-jarに含めないようにしている。
```
  <build>
    <plugins>
(...)
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-shade-plugin</artifactId>
        <version>3.1.0</version>
        <executions>
          <execution>
            <phase>package</phase>
            <goals>
              <goal>shade</goal>
            </goals>
            <configuration>
(...)
              <filters>
                <filter>
                  <!-- NOTE : Bouncy Castle jar file contains META-INF/BCKEY.(DSA|SF) files, which cause
                  'A JNI error has occurred, please check your installation and try again' +
                  'Exception in thread "main" java.lang.SecurityException: Invalid signature file digest for Manifest main attributes'
                  errors.
                  -> filter-out these files, then works fine. (But is this good way in security perspective ... ?? )
                  ref:
                  https://stackoverflow.com/questions/22566191/exception-in-thread-main-java-lang-securityexception-invalid-signature-file-d
                  https://stackoverflow.com/questions/30199035/how-can-i-tell-which-signed-jar-is-causing-maven-shade-plugin-to-fail
                  https://stackoverflow.com/questions/999489/invalid-signature-file-when-attempting-to-run-a-jar
                  https://stackoverflow.com/questions/43201230/create-dependency-folder-with-dependent-jars-with-maven-shade-plugin
                  https://gist.github.com/leewin12/6505726
                  # ooooooooops... lot of "SO" threads ... X(
                  -->
                  <artifact>*:*</artifact>
                  <excludes>
                    <exclude>META-INF/*.SF</exclude>
                    <exclude>META-INF/*.DSA</exclude>
                    <exclude>META-INF/*.RSA</exclude>
                  </excludes>
                </filter>
              </filters>
```

ビルドと実行
```
cd 04_shaded-uber-with-signed-jar/
./mvnw package
java -jar target/bcprov-demo-1.0.0.jar
```

pom.xmlから上記 `<filters>` 要素を削除してビルドしたjarを実行すると、以下のエラーが表示される。
```
java -jar target/bcprov-demo-1.0.0.jar
Error: A JNI error has occurred, please check your installation and try again
Exception in thread "main" java.lang.SecurityException: Invalid signature file digest for Manifest main attributes
        at sun.security.util.SignatureFileVerifier.processImpl(SignatureFileVerifier.java:314)
        at sun.security.util.SignatureFileVerifier.process(SignatureFileVerifier.java:268)
        at java.util.jar.JarVerifier.processEntry(JarVerifier.java:316)
        at java.util.jar.JarVerifier.update(JarVerifier.java:228)
        at java.util.jar.JarFile.initializeVerifier(JarFile.java:383)
        at java.util.jar.JarFile.getInputStream(JarFile.java:450)
        at sun.misc.URLClassPath$JarLoader$2.getInputStream(URLClassPath.java:977)
        at sun.misc.Resource.cachedInputStream(Resource.java:77)
        at sun.misc.Resource.getByteBuffer(Resource.java:160)
        at java.net.URLClassLoader.defineClass(URLClassLoader.java:454)
        at java.net.URLClassLoader.access$100(URLClassLoader.java:73)
        at java.net.URLClassLoader$1.run(URLClassLoader.java:368)
        at java.net.URLClassLoader$1.run(URLClassLoader.java:362)
        at java.security.AccessController.doPrivileged(Native Method)
        at java.net.URLClassLoader.findClass(URLClassLoader.java:361)
        at java.lang.ClassLoader.loadClass(ClassLoader.java:424)
        at sun.misc.Launcher$AppClassLoader.loadClass(Launcher.java:335)
        at java.lang.ClassLoader.loadClass(ClassLoader.java:357)
        at sun.launcher.LauncherHelper.checkAndLoadMain(LauncherHelper.java:495)
```

エラー内容からjarのMETA-INF内の署名が無効であるため、実行に失敗したことが分かる。

- この他、Service Provider周りの設定が埋め込まれたjarファイルなどでもしかしたらuber-jarと相性の悪いケースがあるかもしれない。
- そもそも上記pom.xmlに組み込んだ `<filters>` 要素の設定自体、署名が必要なjarファイルから署名情報をstripしているので、セキュリティを低下させている。
- そのため、依存するjarファイルを「無加工で」パッケージングするのであれば、次に紹介する Maven Application Assembler + Maven Assembly Plugin によるパッケージングや「その他の参考資料」に載せたOne-JARなどの技法を検討する必要がある。

## 05. Maven Application Assembler と Maven Assembly Plugin によるラッパースクリプトの生成とパッケージング

依存するjarファイルを加工せずにそのままパッケージングし、利用者が手軽にパッケージングする方法として、[Maven Application Assembler](http://www.mojohaus.org/appassembler/) と [Maven Assembly Plugin](http://maven.apache.org/plugins/maven-assembly-plugin/index.html) を組み合わせるやり方がある。

- Maven Application Assembler
  - http://www.mojohaus.org/appassembler/
  - https://github.com/mojohaus/appassembler
  - jarファイルのリストを抽出してclasspathに設定するラッパースクリプトを自動生成することができる。
- Maven Assembly Plugin
  - http://maven.apache.org/plugins/maven-assembly-plugin/
  - 依存するjarファイルや必要なファイルをまとめてアーカイブすることができる。

2つのプラグインを組み合わせることで、ラッパースクリプト + 依存jarファイル + その他設定・ドキュメントファイルなどを tar/zip 等にパッケージングすることができる。
イメージとしてはTomcatやMavenの配布用binパッケージの内容と似た感じになり、ユーザとしては展開したのちコマンドプロンプトやshellからラッパースクリプトを起動するだけとなる。
以下に `05_bin-packaging` のサンプルで試したポイントを紹介する。

pom.xml で appassembler-maven-plugin を組み込む。
`<configuration>` の `<program>` 要素設定で、id要素でラッパースクリプトのbasenameと実行クラス名を指定し、`<execution>` 要素で `package` フェーズで実行されるようにしている。
```
(...)
  <build>
    <plugins>
(...)
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>appassembler-maven-plugin</artifactId>
        <version>1.10</version>
        <configuration>
          <!-- appassembler自身は依存jarは収集しない -->
          <generateRepository>false</generateRepository>
          <!-- assembly側のdependencySetで、"/lib" 以下に依存jarがflat構成で収集されるのに合わせる -->
          <repositoryName>lib</repositoryName>
          <repositoryLayout>flat</repositoryLayout>
          <programs>
            <program>
              <mainClass>${exec.mainClass}</mainClass>
              <id>assembly-demo-boot</id>
            </program>
          </programs>
        </configuration>
        <executions>
          <execution>
            <phase>package</phase>
            <goals>
              <goal>assemble</goal>
             </goals>
          </execution>
        </executions>
      </plugin>
(...)
```

これにより `package` フェーズを実行すると `target/appassembler/bin/` に以下のファイルが生成される。
```
assembly-demo-boot : Linux/Mac用shell script
assembly-demo-boot.bat : Windows用BATファイル
```

これらのラッパースクリプトを `bin` フォルダに配置し、jarファイルを `lib` フォルダにまとめたフォルダツリーを zip や tar にまとめるのが Maven Assembly Plugin の役目となる。

まず pom.xml の `<build>` - `<plugins>` に以下の設定を追加し、`package` フェーズで Maven Assembly Plugin の single ゴールが実行されるようにする。
細かい設定は `src/assembly/bin.xml` で設定する。
```
      <plugin>
        <artifactId>maven-assembly-plugin</artifactId>
        <version>3.1.0</version>
        <configuration>
          <descriptors>
            <descriptor>src/assembly/bin.xml</descriptor>
          </descriptors>
        </configuration>
        <executions>
          <execution>
            <id>make-assembly</id> <!-- this is used for inheritance merges -->
            <phase>package</phase> <!-- bind to the packaging phase -->
            <goals>
              <goal>single</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
```

今回のサンプルでは `src/assembly/bin.xml` を以下のように設定した。
```
<assembly xmlns="http://maven.apache.org/ASSEMBLY/2.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/ASSEMBLY/2.0.0 http://maven.apache.org/xsd/assembly-2.0.0.xsd">
  <id>bin</id>
  <formats>
    <format>tar.gz</format>
    <format>zip</format>
  </formats>
  <fileSets>
    <!-- appassemblerにより生成されたbat/shは"/bin"以下に配置 -->
    <fileSet>
      <directory>target/appassembler/bin</directory>
      <outputDirectory>bin</outputDirectory>
    </fileSet>
  </fileSets>
  <dependencySets>
    <dependencySet>
      <outputDirectory>lib</outputDirectory>
      <includes>
        <include>*:jar:*</include>
      </includes>
    </dependencySet>
  </dependencySets>
</assembly>
```

実際にビルドしてみる。
```
cd 05_bin-packaging/
./mvnw package
-> target/ 以下に次のファイルが生成される。
assembly-demo-1.0.0.jar
assembly-demo-1.0.0-bin.tar.gz
assembly-demo-1.0.0-bin.zip
```

試しに `assembly-demo-1.0.0-bin.zip` を展開してみると以下のようなファイルが展開される。
```
assembly-demo-1.0.0/
  bin/
    assembly-demo-boot
    assembly-demo-boot.bat
  lib/
    animal-sniffer-annotations-1.14.jar
    assembly-demo-1.0.0.jar
    bcprov-jdk15on-1.58.jar
    checker-compat-qual-2.0.0.jar
    error_prone_annotations-2.1.3.jar
    guava-23.6-jre.jar
    j2objc-annotations-1.1.jar
    jsr305-1.3.9.jar
```

javaにPATHが通ったコマンドプロンプトまたはshellで、`bin/assembly-demo-boot(.bat)` を実行すれば、アプリケーションが起動する。

なお今回は `<program>` 要素を1つしか設定していないが、例えばサーバ用に `xxxx-startup` と `xxxx-shutdown` など2つ以上のそれぞれ別のメインクラスを起動するスクリプトを生成することもできる。
これにより、Tomcatのようなサーバアプリケーションを、利用者がパッケージをダウンロードしたらすぐに使うことができる状態で配布する道が開ける。

日本語参考記事
- Maven でアプリケーション実行用バッチファイルを作る - A Memorandum
  - http://etc9.hatenablog.com/entry/20101206/1291619754
- Java/jarファイルの配布と実行方式
  - https://www.glamenv-septzen.net/view/1121
  - 手前味噌で記事も古くURLや設定が古かったりするが、考え方や大枠は流用できる。
  - 実際に簡単なデーモンアプリケーションで起動用/停止用の複数のラッパースクリプトを生成するデモを紹介している。

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




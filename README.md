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
- そのため、依存するjarファイルを「無加工で」パッケージングするのであれば、後述の Maven Assembly Plugin によるパッケージングや「その他の参考資料」に載せたOne-JARなどの技法を検討する必要がある。





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




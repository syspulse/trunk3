import sbt._

object Dependencies {

    // lazy val scala = "2.13.9"
    lazy val scala = "2.13.15"
    lazy val nashornVersion = "15.6"

    // Versions
    lazy val versionScalaLogging = "3.9.2"
    lazy val akkaVersion    = "2.6.20"
    lazy val alpakkaVersion = "3.0.4"  
    lazy val akkaHttpVersion = "10.2.9" //"10.2.4"
    lazy val akkaKafkaVersion = "2.0.3"
    lazy val kafkaAvroSerVersion = "5.4.1"
    lazy val quillVersion = "3.6.0"
    lazy val influxDBVersion = "3.2.0"
    lazy val slickVersion = "3.3.3"
    lazy val sparkVersion = "3.2.0"
    lazy val hadoopAWSVersion = "3.2.2"
    lazy val janinoVersion = "3.0.16" //"3.1.6" //"3.0.16"
    lazy val elastic4sVersion = "7.17.3"

    lazy val skelVersion = "0.13.0"
    lazy val extVersion = "0.13.0"

    lazy val appVersion = "0.13.0"
    lazy val jarPrefix = ""
    
    lazy val appDockerRoot = "/app"

    val libNashorn = "org.openjdk.nashorn" % "nashorn-core" % nashornVersion

    // Akka Libraries
    val libAkkaActor =      "com.typesafe.akka"           %% "akka-actor"           % akkaVersion
    val libAkkaActorTyped = "com.typesafe.akka"           %% "akka-actor-typed"     % akkaVersion
    val libAkkaCluster =    "com.typesafe.akka"           %% "akka-cluster"         % akkaVersion
    val libAkkaHttp =       "com.typesafe.akka"           %% "akka-http"            % akkaHttpVersion
    val libAkkaHttpSpray =  "com.typesafe.akka"           %% "akka-http-spray-json" % akkaHttpVersion
    val libAkkaStream =     "com.typesafe.akka"           %% "akka-stream"          % akkaVersion

    val libAkkaPersistence ="com.typesafe.akka"           %% "akka-persistence-typed"         % akkaVersion
    val libAkkaPersistenceTest ="com.typesafe.akka"       %% "akka-persistence-testkit"       % akkaVersion % Test

    val libAkkaSerJackon =  "com.typesafe.akka"           %% "akka-serialization-jackson"     % akkaVersion
    //val libAkkaSerProtobuf ="com.typesafe.akka"           %% "akka-serialization-protobuf"    % akkaVersion

    val libAlpakkaInfluxDB ="com.lightbend.akka"          %% "akka-stream-alpakka-influxdb"       % alpakkaVersion
    val libAlpakkaCassandra="com.lightbend.akka"          %% "akka-stream-alpakka-cassandra"      % alpakkaVersion //"2.0.2"
    val libAlpakkaDynamo=   "com.lightbend.akka"          %% "akka-stream-alpakka-dynamodb"       % alpakkaVersion
    val libAlpakkaElastic=  "com.lightbend.akka"          %% "akka-stream-alpakka-elasticsearch"  % alpakkaVersion
    val libAlpakkaMQTT=     "com.lightbend.akka"          %% "akka-stream-alpakka-mqtt-streaming" % alpakkaVersion

    val libAkkaProtobuf =   "com.typesafe.akka"               %% "akka-protobuf"        % akkaVersion
    val libAkkaKafka=       "com.typesafe.akka"               %% "akka-stream-kafka"    % akkaKafkaVersion

    val libAkkaHttpCors =   "ch.megard"                       %% "akka-http-cors"       % "1.1.3"

    val libScalaLogging =   "com.typesafe.scala-logging"      %% "scala-logging"        % "3.9.2"
    val libLogback =        "ch.qos.logback"                  % "logback-classic"      % "1.3.5" //"1.2.8"
    val libJanino =         "org.codehaus.janino"             % "janino"               % janinoVersion
    // I need this rubbish slf4j to deal with old jboss dependecny which generates exception in loading logback.xml
    //val libSlf4jApi =       "org.slf4j"                   %  "slf4j-api"            % "1.8.0-beta4"
    // Supports only old XML Config file format
    // val libSlf4jApi =       "org.slf4j"                       % "slf4j-api"            % "1.7.26"
    val libSlf4jApi =       "org.slf4j"                       % "slf4j-api"            % "2.0.5"
    // Needed for teku
    val libLog4j2Api =      "org.apache.logging.log4j"        % "log4j-api" % "2.17.2"
    val libLog4j2Core =     "org.apache.logging.log4j"        % "log4j-core" % "2.17.2"

    val libQuill =          "io.getquill"                     %% "quill-jdbc"           % "3.5.2"
    val libMySQL =          "mysql"                           % "mysql-connector-java" % "8.0.22"
    val libPostgres =       "org.postgresql"                  % "postgresql"            % "42.3.5"
    val libInfluxDB =       "com.influxdb"                    %% "influxdb-client-scala" % influxDBVersion

    val libTypesafeConfig = "com.typesafe"                    % "config"               % "1.4.1"
      
    val libWsRsJakarta =    "jakarta.ws.rs"                   % "jakarta.ws.rs-api"     % "3.1.0" //"3.0.0"
    val libSwaggerAkkaHttp ="com.github.swagger-akka-http"    %% "swagger-akka-http"    % "2.7.0"
    
    val libMetrics =        "nl.grons"                        %% "metrics4-scala"       % "4.1.14"
    //val libAkkaHttpMetrics ="fr.davit"                      %% "akka-http-metrics-dropwizard" % "1.6.0"
    val libAkkaHttpMetrics ="fr.davit"                        %% "akka-http-metrics-prometheus" % "1.6.0"

      // "org.backuity.clist" %% "clist-core"               % "3.5.1",
      // "org.backuity.clist" %% "clist-macros"             % "3.5.1" % "provided",
    val libScopt =          "com.github.scopt"                %% "scopt"                % "4.0.0"

    val libUUID =           "io.jvm.uuid"                     %% "scala-uuid"           % "0.3.1"

    val libKafkaAvroSer =   "io.confluent"                    % "kafka-avro-serializer" % kafkaAvroSerVersion

    val libScalaTest =      "org.scalatest"                   %% "scalatest"            % "3.1.2" //"3.12.3" // % Test
    //val libSpecs2core =     "org.specs2"                    %% "specs2-core"          % "2.4.17"
    val libAkkaTestkit =    "com.typesafe.akka"               %% "akka-http-testkit"        % akkaHttpVersion// % Test
    val libAkkaTestkitType ="com.typesafe.akka"               %% "akka-actor-testkit-typed" % akkaVersion// % Test
    
    val libJline =          "org.jline"                       % "jline"                 % "3.14.1"
    //val libJson4s =         "org.json4s"                    %%  "json4s-native"        % "3.6.7"
    val libOsLib =          "com.lihaoyi"                     %% "os-lib"               % "0.8.0" //"0.7.7"
    val libUpickleLib =     "com.lihaoyi"                     %% "upickle"              % "1.4.1"
    //val libUjsonLib =       "com.lihaoyi"                   %% "ujson"                % "1.4.1"
    val libScalaTags =      "com.lihaoyi"                     %% "scalatags"            % "0.9.4"
    val libCask =           "com.lihaoyi"                     %% "cask"                 % "0.7.11" // "0.7.8"
    val libRequests =       "com.lihaoyi"                     %% "requests"             % "0.6.9"

    val libCsv =            "com.github.tototoshi"            %% "scala-csv"            % "1.3.7"
    val libFaker =          "com.github.javafaker"            % "javafaker"             % "1.0.2"

    val libPrometheusClient =   "io.prometheus"               % "simpleclient"              % "0.10.0"
    val libPrometheusHttp =     "io.prometheus"               % "simpleclient_httpserver"   % "0.10.0"
    val libPrometheusHotspot =  "io.prometheus"               % "simpleclient_hotspot"   % "0.10.0"
    //val libPrometheusPushGw = "io.prometheus"               % "simpleclient_pushgateway"   % "0.10.0"
    
    // This is modified version for Scala2.13 (https://github.com/syspulse/kuro-otp)
    val libKuroOtp =        "com.ejisan"                      %% "kuro-otp"           % "0.0.4-SNAPSHOT"
    val libQR =             "net.glxn"                        % "qrgen"               % "1.4"

    // val libWeb3jCrypto =    "org.web3j"                     % "crypto"              % "4.8.7" exclude("org.bouncycastle", "bcprov-jdk15on")
    // val libWeb3jCore =      "org.web3j"                     % "core"                % "4.8.7" exclude("org.bouncycastle", "bcprov-jdk15on")
    val libWeb3jCrypto =    "org.web3j"                       % "crypto"              % "4.9.2" exclude("org.bouncycastle", "bcprov-jdk15on")
    val libWeb3jCore =      "org.web3j"                       % "core"                % "4.9.2" exclude("org.bouncycastle", "bcprov-jdk15on")
    
    //web3j depends on "1.65"
    val libBouncyCastle =   "org.bouncycastle"                % "bcprov-jdk15on"      % "1.70" //"1.69" 
    
    val libScodecBits =     "org.scodec"                      %% "scodec-bits"        % "1.1.30" //"1.1.12" 
    val libHKDF =           "at.favre.lib"                    % "hkdf"                % "1.1.0"
    val libBLS =            "tech.pegasys.teku.internal"      % "bls"                 % "23.3.1" //"21.9.2"
    val libBLSKeystore =    "tech.pegasys.signers.internal"   % "bls-keystore"        % "2.2.1"  //"1.0.21"

    val libScalaScraper =   "net.ruippeixotog"                %% "scala-scraper"      % "2.2.1"

    val libQuartz =         "org.quartz-scheduler"            % "quartz"              % "2.3.2" exclude("com.zaxxer", "HikariCP-java7")

    val libTwitter4s =      "com.danielasfregola"             %% "twitter4s"          % "7.0"
    
    val libSeleniumJava =   "org.seleniumhq.selenium"         % "selenium-java"             % "4.0.0-rc-3"
    val libSeleniumFirefox ="org.seleniumhq.selenium"         % "selenium-firefox-driver"   % "4.0.0-rc-3"

    val libJwtCore =        "com.pauldijou"                   %% "jwt-core"           % "4.2.0"
    val libJoseJwt =        "com.nimbusds"                    % "nimbus-jose-jwt"     % "4.21"

    val libAvro4s =         "com.sksamuel.avro4s"             %% "avro4s-core"        % "4.0.12"

    val libSSSS =           "com.gladow"                      %% "scalassss"          % "0.2.0-SNAPSHOT"

    val libSparkCore =       "org.apache.spark"               %% "spark-core"         % sparkVersion
    val libSparkSQL =        "org.apache.spark"               %% "spark-sql"          % sparkVersion
    val libHadoopAWS =       "org.apache.hadoop"              % "hadoop-aws"          % hadoopAWSVersion
    val libAWSJavaSDK =      "com.amazonaws"                  % "aws-java-sdk-bundle" % "1.11.874" //"1.12.247"
    val libJaninoCompiler =  "org.codehaus.janino"            %  "commons-compiler"   % janinoVersion

    val libFlyingSaucer =    "org.xhtmlrenderer"              %  "flying-saucer-pdf-itext5"   % "9.1.22"
    val libThymeleaf =       "org.thymeleaf"                  % "thymeleaf"                   % "3.0.11.RELEASE"
    val libNekoHtml =        "net.sourceforge.nekohtml"       % "nekohtml"                    % "1.9.21"
    val libJSoup =           "org.jsoup"                      % "jsoup"                       % "1.15.1"
    val libLaikaCore =       "org.planet42"                   %% "laika-core"                 % "0.18.2"
    val libLaikaIo =         "org.planet42"                   %% "laika-io"                   % "0.18.2"
    //val libLaikaPdf =        "org.planet42"                 %% "laika-pdf"                  % "0.18.2"
    
    val libCasbin =          "org.casbin"                     % "jcasbin"                     % "1.7.1"

    val libAkkaPersistJDBC =  "com.lightbend.akka" %% "akka-persistence-jdbc" % "5.0.4"
    //val libAkkaPersistQuery = "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion
    val libSlick =            "com.typesafe.slick" %% "slick" % slickVersion
    val libSlickHikari =      "com.typesafe.slick" %% "slick-hikaricp" % slickVersion
    val libH2 =               "com.h2database" % "h2" % "1.4.200"                     //% Test
    val libLevelDB =          "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"  % Test
    val libElastic4s =        "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % elastic4sVersion

    val libStellar =          "com.github.stellar" %% "java-stellar-sdk" % "0.43.0"

    // Projects
    val libAkka = Seq(libAkkaActor,libAkkaActorTyped,libAkkaStream)
    val libAlpakka = Seq(libAlpakkaInfluxDB)
    val libPrometheus = Seq(libPrometheusClient,libPrometheusHttp,libPrometheusHotspot)
    val libHttp = Seq(libAkkaHttp,libAkkaHttpSpray,libAkkaHttpMetrics,libAkkaHttpCors,libWsRsJakarta,libSwaggerAkkaHttp) ++ libPrometheus
    val libCommon = Seq(libScalaLogging, libSlf4jApi, libLogback, libJanino, libTypesafeConfig )
    
    val libTest = Seq(libOsLib, libScalaTest % Test,libAkkaTestkit % Test,libAkkaTestkitType % Test)
    val libTestLib = Seq(libScalaTest,libAkkaTestkit,libAkkaTestkitType)

    val libSkel = Seq(libMetrics,libScopt,libUUID)

    val libDB = Seq(libQuill,libMySQL, libPostgres)

    val libLihaoyi = Seq(libOsLib,libUpickleLib)

    val libWeb3j = Seq(libBouncyCastle,libWeb3jCore,libWeb3jCrypto)

    val libJwt = Seq(libJwtCore,libJoseJwt)

    val libSpark = Seq(libSparkCore,libSparkSQL,libJanino,libJaninoCompiler)
    val libSparkAWS = libSpark ++ Seq(libHadoopAWS,libAWSJavaSDK)

    val libPdfGen = Seq(libFlyingSaucer,libThymeleaf,libNekoHtml,libJSoup)
    

    val libSkelCore =         "io.syspulse"                     %% "skel-core"                      % skelVersion
    val libSkelAuthCore =     "io.syspulse"                     %% "skel-auth-core"                 % skelVersion
    val libSkelCrypto =       "io.syspulse"                     %% "skel-crypto"                    % skelVersion
    val libSkelIngest =       "io.syspulse"                     %% "skel-ingest"                    % skelVersion
    //val libSkelIngestFlow =   "io.syspulse"                     %% "ingest-flow"                    % skelVersion
    val libSkelIngestElastic ="io.syspulse"                     %% "ingest-elastic"                 % skelVersion
    val libSkelDSL =          "io.syspulse"                     %% "skel-dsl"                       % skelVersion
    val libSkelNotify =       "io.syspulse"                     %% "skel-notify"                    % skelVersion
    val libSkelNotifyCore =   "io.syspulse"                     %% "notify-core"                    % skelVersion
    val libSkelCli =          "io.syspulse"                     %% "skel-cli"                       % skelVersion
    val libSkelSerde =        "io.syspulse"                     %% "skel-serde"                     % skelVersion
    val libSkelOdometer =     "io.syspulse"                     %% "skel-odometer"                  % skelVersion
    val libSkelBlockchainCore="io.syspulse"                     %% "blockchain-core"                % skelVersion
    val libSkelBlockchainEvm ="io.syspulse"                     %% "blockchain-evm"                 % skelVersion
    val libSkelBlockchainTron ="io.syspulse"                    %% "blockchain-tron"                % skelVersion
    
    val libSkelSyslogCore =   "io.syspulse"                     %% "syslog-core"                    % skelVersion
    val libSkelJobCore =      "io.syspulse"                     %% "job-core"                       % skelVersion
    
    val libEthAbi =           "com.github.lbqds"                %% "ethabi"                         % "0.4.1"

    val libExtCore =          "io.syspulse"                     %% "ext-core"                       % extVersion
  }
  
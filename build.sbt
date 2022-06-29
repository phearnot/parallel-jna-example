val levelDbVersion = "1.23.1"

libraryDependencies ++= Seq(
//  "net.java.dev.jna" % "jna" % "5.12.0",
  "com.google.guava" % "guava" % "31.1-jre",
  "com.wavesplatform.leveldb-jna" % "leveldb-jna-core"   % levelDbVersion,
  "com.wavesplatform.leveldb-jna" % "leveldb-jna-native" % levelDbVersion
)

run / fork := true
Compile / run / mainClass := Some("com.wavesplatform.TestApp")

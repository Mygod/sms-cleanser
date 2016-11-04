scalaVersion := "2.11.8"

enablePlugins(AndroidApp)
android.useSupportVectors

name := "sms-cleanser"
version := "0.0.0"
versionCode := Some(412)

minSdkVersion := "19"
platformTarget := "android-25"

compileOrder := CompileOrder.JavaThenScala
javacOptions ++= "-source" :: "1.7" :: "-target" :: "1.7" :: Nil
scalacOptions ++= "-target:jvm-1.7" :: "-Xexperimental" :: Nil

proguardVersion := "5.3.1"
proguardCache := Seq()

shrinkResources := true
typedViewHolders := false
resConfigs := Seq("zh-rCN")

resolvers ++= Seq(Resolver.jcenterRepo, Resolver.sonatypeRepo("public"))
libraryDependencies ++=
  "com.android.support" % "support-v13" % "25.0.0" ::
  "be.mygod" %% "mygod-lib-android" % "4.0.3" ::
  Nil

plugins {
    application
    kotlin("jvm") version "1.3.61"
    kotlin("kapt") version "1.3.61"
}

group = "fr.matk"
version = "1.0-SNAPSHOT"

application {
    mainClassName = "fr.matk.MainKt"
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    compile(kotlin("stdlib"))
    compile(kotlin("reflect"))

    // Rx
    compile("io.reactivex.rxjava2:rxkotlin:2.4.0")
    compile("io.reactivex.rxjava2:rxjava:2.2.15")

    // DI
    compile("org.koin:koin-core:2.0.1")

    // CLI
    compile("com.github.ajalt:clikt:2.3.0")

    // Network
    compile("com.squareup.okhttp3:okhttp:4.2.2")
    compile("com.squareup.moshi:moshi-kotlin:1.9.2")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.9.2")

    // Logs
    compile("org.slf4j:slf4j-api:1.7.28")
    compile("ch.qos.logback:logback-classic:1.2.3")

    testImplementation("org.junit.jupiter:junit-jupiter:5.4.2")
}

tasks {
    withType<Jar> {
        manifest {
            attributes(mapOf("Main-Class" to application.mainClassName))
        }

        // Add all dependency in source path to build FatJar
        from(configurations.compile.get().map { if (it.isDirectory) it else zipTree(it) })
    }
}

plugins {
    application
    kotlin("jvm") version "1.3.21"
}

group = "fr.matk"
version = "1.0-SNAPSHOT"

application {
    mainClassName = "fr.matk.MainKt"
}

repositories {
    mavenCentral()
}

dependencies {
    compile(kotlin("stdlib"))
    compile(kotlin("reflect"))

    compile("com.github.ajalt:clikt:2.3.0")
    compile("io.reactivex.rxjava2:rxkotlin:2.4.0")
    compile("io.reactivex.rxjava2:rxjava:2.2.15")
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

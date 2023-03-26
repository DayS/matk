plugins {
    application
    kotlin("jvm") version "1.8.10"
    kotlin("kapt") version "1.8.10"
}

group = "fr.matk"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("fr.matk.MainKt")
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of("11"))
    }
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    // Rx
    implementation("io.reactivex.rxjava2:rxkotlin:2.4.0")
    implementation("io.reactivex.rxjava2:rxjava:2.2.15")

    // DI
    implementation("org.koin:koin-core:2.0.1")

    // CLI
    implementation("com.github.ajalt:clikt:2.8.0")

    // Network
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.14.0")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.14.0")

    // Logs
    implementation("org.slf4j:slf4j-api:1.7.28")
    implementation("ch.qos.logback:logback-classic:1.4.6")

    // Json
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.4.2")
}

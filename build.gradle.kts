plugins {
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.serialization") version "1.6.21"
    java
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    // Logging
    implementation("org.slf4j", "slf4j-api", "1.7.36")
    implementation("org.slf4j", "slf4j-simple", "1.7.36")

    // LuceLang serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")

    // TU PROLOG
    implementation("it.unibo.tuprolog", "core-jvm", "0.20.4")
    implementation("it.unibo.tuprolog", "unify-jvm", "0.20.4")
    implementation("it.unibo.tuprolog", "theory-jvm", "0.20.4")
    implementation("it.unibo.tuprolog", "solve-jvm", "0.20.4") // resolution solver
    implementation("it.unibo.tuprolog", "solve-classic-jvm", "0.20.4") // SLDNF resolution implementation

    implementation("it.unibo.tuprolog", "parser-jvm", "0.20.4")
    implementation("it.unibo.tuprolog", "parser-core-jvm", "0.20.4")
    implementation("it.unibo.tuprolog", "parser-theory-jvm", "0.20.4")

    // implementation("it.unibo.tuprolog", "serialize-core-jvm", "0.20.4")
    // implementation("it.unibo.tuprolog", "serialize-theory-jvm", "0.20.4")

    // implementation("it.unibo.tuprolog", "dsl-core-jvm", "0.20.4")
    // implementation("it.unibo.tuprolog", "dsl-solve-jvm", "0.20.4")
    // implementation("it.unibo.tuprolog", "dsl-unify-jvm", "0.20.4")
    // implementation("it.unibo.tuprolog", "dsl-theory-jvm", "0.20.4")

    // Tests
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("org.springframework:spring-test:5.3.18")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
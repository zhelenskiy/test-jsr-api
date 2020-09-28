import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.0"
}
group = "me.zhelenskiy"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}
dependencies {
    implementation(kotlin("main-kts"))
    implementation(kotlin("scripting-jsr223"))

    implementation(kotlin("scripting-jvm-host"))
    implementation(kotlin("reflect"))
    implementation(kotlin("scripting-common"))
    implementation(kotlin("compiler-embeddable"))

    testImplementation(kotlin("test-junit5"))
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
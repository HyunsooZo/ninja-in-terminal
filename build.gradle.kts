plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "com.ninja.terminal"
version = "1.0.0"

repositories {
    mavenCentral()
    maven { url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies") }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.graphics")
}

dependencies {
    // SSH
    implementation("com.jcraft:jsch:0.1.55")

    // JediTerm - Terminal Emulator (버전 통일)
    implementation("org.jetbrains.jediterm:jediterm-core:3.47")
    implementation("org.jetbrains.jediterm:jediterm-ui:3.47")

    // JSON
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.0")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.12")
    implementation("ch.qos.logback:logback-classic:1.5.13")
}

application {
    mainClass.set("com.ninja.terminal.app.MainApp")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.ninja.terminal.app.MainApp"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // Fat jar 구성 (runtimeClasspath 포함)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.exists() }
            .map { if (it.isDirectory) it else zipTree(it) }
    })
}

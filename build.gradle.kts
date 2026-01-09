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
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.graphics", "javafx.swing")
}

application {
    // [중요] module-info.java가 없다면 이 줄은 주석 처리하거나 지워야 합니다.
    // mainModule.set("com.ninja.terminal")

    // [핵심 수정] MainApp이 아닌 Launcher를 실행하도록 변경
    mainClass.set("com.ninja.terminal.app.Launcher")
}

dependencies {
    // SSH
    implementation("com.jcraft:jsch:0.1.55")

    // JediTerm - Terminal Emulator
    implementation("org.jetbrains.jediterm:jediterm-core:3.47")
    implementation("org.jetbrains.jediterm:jediterm-ui:3.47")

    // JSON
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.0")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.12")
    implementation("ch.qos.logback:logback-classic:1.5.13")
}

tasks.jar {
    manifest {
        // [핵심 수정] Jar 파일 실행 시에도 Launcher가 시작점이어야 함
        attributes["Main-Class"] = "com.ninja.terminal.app.Launcher"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // Fat jar 구성 (의존성 포함)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.exists() }
            .map { if (it.isDirectory) it else zipTree(it) }
    })
}
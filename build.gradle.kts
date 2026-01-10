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

// Windows Portable 패키징
tasks.register<Exec>("createWindowsPortable") {
    group = "distribution"
    description = "Create Windows portable package with jpackage"
    
    dependsOn("jar")
    
    val portableDir = file("$buildDir/portable/TerminalInNinja")
    
    doFirst {
        portableDir.deleteRecursively()
        portableDir.mkdirs()
    }
    
    commandLine = listOf(
        "jpackage",
        "--name", "TerminalInNinja",
        "--input", file("build/libs").absolutePath,
        "--main-jar", "ninja-in-terminal-${version}.jar",
        "--main-class", "com.ninja.terminal.app.MainApp",
        "--type", "app-image",
        "--dest", file("$buildDir/portable").absolutePath,
        "--icon", file("src/main/resources/images/ninja-exe-icon.ico").absolutePath,
        "--win-console"
    )
}

// 단축아이콘 생성 스크립트 복사
tasks.register<Copy>("copyShortcutScripts") {
    group = "distribution"
    description = "Copy shortcut creation scripts to portable package"
    
    dependsOn("createWindowsPortable")
    
    from("scripts/windows")
    include("*.vbs")
    into(file("$buildDir/portable/TerminalInNinja"))
}

// README 복사
tasks.register<Copy>("copyReadme") {
    group = "distribution"
    description = "Copy README to portable package"
    
    dependsOn("createWindowsPortable")
    
    from("docs")
    include("WINDOWS-README.txt")
    into(file("$buildDir/portable/TerminalInNinja"))
    rename { "README.txt" }
}

// ZIP 파일 생성
tasks.register<Zip>("createPortableZip") {
    group = "distribution"
    description = "Create portable ZIP package"
    
    dependsOn("copyShortcutScripts", "copyReadme")
    
    archiveFileName.set("TerminalInNinja-${version}-windows-x64.zip")
    destinationDirectory.set(file("$buildDir/distributions"))
    from(file("$buildDir/portable/TerminalInNinja"))
}
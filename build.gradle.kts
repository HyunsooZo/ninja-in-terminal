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

// Windows Portable 패키징 (통합 버전)
tasks.register<Exec>("createWindowsPortable") {
    group = "distribution"
    description = "Create Windows portable package with jpackage"

    dependsOn("jar")

    val portableDir = file("$buildDir/portable/NinjaInTerminal")

    doFirst {
        // 기존 폴더가 있다면 삭제 (jpackage 충돌 방지)
        portableDir.deleteRecursively()
    }

    commandLine = listOf(
        "jpackage",
        "--name", "NinjaInTerminal",
        "--input", file("build/libs").absolutePath,
        "--main-jar", "ninja-in-terminal-${version}.jar",
        "--main-class", "com.ninja.terminal.app.Launcher",
        "--type", "app-image",
        "--dest", file("$buildDir/portable").absolutePath,
        "--icon", file("src/main/resources/images/ninja-exe-icon.ico").absolutePath,
        "--win-console"
    )

    // jpackage 완료 후, 같은 태스크 안에서 파일 복사를 수행 (에러 방지)
    doLast {
        println("Copying additional files to portable directory...")

        // 1. 스크립트 복사
        project.copy {
            from("scripts/windows")
            include("*.vbs")
            into(portableDir)
        }

        // 2. README 복사
        project.copy {
            from("docs")
            include("WINDOWS-README.txt")
            into(portableDir)
            rename { "README.txt" }
        }
    }
}

// ZIP 파일 생성
tasks.register<Zip>("createPortableZip") {
    group = "distribution"
    description = "Create portable ZIP package"

    dependsOn("createWindowsPortable")

    archiveFileName.set("NinjaInTerminal-${version}-windows-x64.zip")
    destinationDirectory.set(file("$buildDir/distributions"))

    // portable 폴더 내용을 ZIP으로 압축
    from(file("$buildDir/portable")) {
        include("NinjaInTerminal/**")
    }
}


plugins {
    id("java")
    id("application")
    id("antlr")
    id("com.diffplug.spotless") version "7.0.3"
}

spotless {
    encoding(Charsets.UTF_8) // <- correct
    lineEndings = com.diffplug.spotless.LineEnding.UNIX

    java {
        leadingSpacesToTabs()
        importOrder()
        removeUnusedImports()
        eclipse().configFile(file("${rootProject.projectDir}/spotless-formatting.xml"))
        target("src/**/*.java")
        targetExclude(
            "**/build/generated/**/*.java",
            "**/build/generated-src/**/*.java",
            "**/target/generated-sources/**/*.java",
            "**/VersionInfo.java"
        )
    }

    antlr4 {
        target("src/*/antlr/**/*.g4")
        antlr4Formatter()
    }
}


// Apply code-style tasks
apply(from = "code-style.gradle.kts")

group = "it.unive.jlisa"
version = "0.1"

application {
    mainClass.set("it.unive.jlisa.Main")
}

repositories {
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/lisa-analyzer/lisa")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
        }
    }
}

dependencies {
    antlr("org.antlr:antlr4:4.8-1")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("junit:junit:4.13.1")

    implementation("org.eclipse.jdt:org.eclipse.jdt.core:3.41.0")
    implementation("commons-cli:commons-cli:1.4")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("io.github.lisa-analyzer:lisa-sdk:0.2-svcomp-SNAPSHOT")
    implementation("io.github.lisa-analyzer:lisa-analyses:0.2-svcomp-SNAPSHOT")
    implementation("io.github.lisa-analyzer:lisa-program:0.2-svcomp-SNAPSHOT")
    implementation("io.github.classgraph:classgraph:4.8.175")
}

// ANTLR
tasks.named<org.gradle.api.plugins.antlr.AntlrTask>("generateGrammarSource") {
    maxHeapSize = "64m"
    arguments.addAll(listOf("-visitor", "-no-listener"))

    doLast {
        copy {
            from("build/generated-src/antlr/main/")
            include("*.*")
            into("build/generated-src/antlr/main/it/unive/jlisa/antlr")
        }
        project.delete(fileTree("build/generated-src/antlr/main") {
            include("*.*")
        })
    }
}

// TEST CONFIGURATION
tasks.test {
    useJUnitPlatform()
}

// JAR (FAT JAR)
tasks.jar {
    manifest {
        attributes(
            "Main-Class" to application.mainClass.get(),
            "Implementation-Version" to project.version
        )
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn("checkCodeStyle")
}


// DISTZIP
tasks.named<Zip>("distZip") {
    dependsOn(tasks.test)
}

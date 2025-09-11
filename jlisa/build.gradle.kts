plugins {
    id("java")
    id("application")
    id("antlr")
    id("com.diffplug.spotless") version "7.0.3"
}
apply(from = "code-style.gradle.kts")

group = "it.unive.jlisa"
version = "1.0-SNAPSHOT"

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
    classpath 'com.diffplug.spotless:spotless-plugin-gradle:7.0.3'
}


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

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }

    // Include classes and resources from all dependencies (fat jar)
    from({
        configurations.runtimeClasspath.get().map { file ->
            if (file.isDirectory) file
            else zipTree(file).matching {
                // Exclude signature files from META-INF
                exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
            }
        }
    })

    // Exclude duplicates and avoid merging signed metadata
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named<Zip>("distZip") {
	dependsOn(tasks.test)
}
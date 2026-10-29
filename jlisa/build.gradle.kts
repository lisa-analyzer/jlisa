

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
    implementation("commons-cli:commons-cli:1.5.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("io.github.lisa-analyzer:lisa-sdk:1.1-svcomp-2027-SNAPSHOT")
    implementation("io.github.lisa-analyzer:lisa-analyses:1.1-svcomp-2027-SNAPSHOT")
    implementation("io.github.lisa-analyzer:lisa-program:1.1-svcomp-2027-SNAPSHOT")
    implementation("io.github.classgraph:classgraph:4.8.175")
    // YAML parsing for v2 witnesses in the witness validator
    implementation("org.yaml:snakeyaml:2.2")
}

// WITNESS VALIDATOR
// The validator runs the programs under JDI: jdk.jdi is not in the default module graph of
// classpath applications, both when compiling and when running.
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("--add-modules", "jdk.jdi"))
}

// bin/jlisa-wit next to bin/jlisa in the distribution, on the same lib folder
val jlisaWitStartScripts by tasks.registering(CreateStartScripts::class) {
    applicationName = "jlisa-wit"
    mainClass.set("it.unive.jlisa.witness.validator.Main")
    defaultJvmOpts = listOf("--add-modules", "jdk.jdi")
    outputDir = layout.buildDirectory.dir("jlisa-wit-scripts").get().asFile
    classpath = tasks.startScripts.get().classpath
}

distributions {
    main {
        contents {
            into("bin") {
                from(jlisaWitStartScripts)
                filePermissions { unix("rwxr-xr-x") }
            }
        }
    }
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

    testLogging {
        showStandardStreams = false
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }

    val outputs = mutableMapOf<String, StringBuilder>() // test key -> output buffer

    fun key(d: org.gradle.api.tasks.testing.TestDescriptor) = "${d.className}.${d.name}"

    addTestOutputListener { descriptor, event ->
        outputs.computeIfAbsent(key(descriptor)) { StringBuilder() }.append(event.message)
    }

    addTestListener(object : org.gradle.api.tasks.testing.TestListener {
        override fun beforeSuite(suite: org.gradle.api.tasks.testing.TestDescriptor) {}
        override fun afterSuite(
            suite: org.gradle.api.tasks.testing.TestDescriptor,
            result: org.gradle.api.tasks.testing.TestResult
        ) {}
        override fun beforeTest(testDescriptor: org.gradle.api.tasks.testing.TestDescriptor) {}
        override fun afterTest(
            desc: org.gradle.api.tasks.testing.TestDescriptor,
            result: org.gradle.api.tasks.testing.TestResult
        ) {
            val buf = outputs.remove(key(desc))
            if (result.resultType == org.gradle.api.tasks.testing.TestResult.ResultType.FAILURE
                && buf != null) {
                println("=== Captured output for ${desc.className}.${desc.name} ===")
                print(buf.toString())
                println("=============================================================")
            }
        }
    })
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

    // Run code style checks before packaging
    dependsOn("checkCodeStyle")
}

// DISTZIP
tasks.named<Zip>("distZip") {
    dependsOn(tasks.test)
}

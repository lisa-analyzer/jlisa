plugins {
    id("java")
    id("application")
}

group = "it.unive.jlisa"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("it.unive.jlisa.Main") // Replace with your actual class if different
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
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("junit:junit:4.13")

    implementation("org.eclipse.jdt:org.eclipse.jdt.core:3.41.0")
    implementation("commons-cli:commons-cli:1.4")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("io.github.lisa-analyzer:lisa-sdk:0.2-svcomp-SNAPSHOT")
    implementation("io.github.lisa-analyzer:lisa-analyses:0.2-svcomp-SNAPSHOT")
    implementation("io.github.lisa-analyzer:lisa-program:0.2-svcomp-SNAPSHOT")
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

val jpackage by tasks.registering(Exec::class) {
    dependsOn(tasks.named("jar"))

    val outputDir = "${buildDir}/jpackage"
    val appName = "jlisa"
    val mainJar = "${project.buildDir}/libs/${appName}-${version}.jar"
    val mainClassName = application.mainClass.get() // Dynamically getting Main-Class

    doFirst {
        mkdir(outputDir)
        val appBundle = file("$outputDir/$appName.app")
        if (appBundle.exists()) {
            println("Deleting existing app bundle: $appBundle")
            appBundle.deleteRecursively()
        }
    }

    // Base jpackage command
    commandLine(
        "jpackage",
        "--input", "${buildDir}/libs",
        "--name", appName,
        "--main-jar", "${appName}-${version}.jar",
        "--main-class", mainClassName,
        "--dest", outputDir,
        "--type", if (System.getProperty("os.name").startsWith("Windows")) "exe" else "app-image",
        "--java-options", "-Xmx512m"
    )
}
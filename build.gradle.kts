plugins {
    id("java")
    id("application")
}

group = "it.unive.jlisa"
version = "1.0.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass.set("it.unive.jlisa.witness.validator.Main")
    // jdk.jdi is not in the default module graph for classpath (non-modular) applications.
    // Specifying it here embeds the flag in the generated start scripts.
    applicationDefaultJvmArgs = listOf("--add-modules", "jdk.jdi")
}

repositories {
    mavenCentral()
    // GitHub Packages: needed for io.github.lisa-analyzer:lisa-sdk (transitive via jLISA).
    // Set credentials in ~/.gradle/gradle.properties: gpr.user / gpr.key
    // or via environment variables USERNAME / TOKEN.
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
    // jLISA — resolved via composite build declared in settings.gradle.kts.
    implementation("it.unive.jlisa:jlisa")

    // LiSA SDK: jLISA declares these as `implementation` (not `api`), so they are NOT
    // transitively visible to consumers of jLISA. We declare them explicitly so that
    // the correctness-validator code can use it.unive.lisa.* types directly.
    // The jars are already in the Gradle module cache from previous jLISA builds.
    implementation("io.github.lisa-analyzer:lisa-sdk:1.1-svcomp-2027-SNAPSHOT")
    implementation("io.github.lisa-analyzer:lisa-analyses:1.1-svcomp-2027-SNAPSHOT")
    implementation("io.github.lisa-analyzer:lisa-program:1.1-svcomp-2027-SNAPSHOT")

    // Eclipse JDT is also transitively hidden and needed for jLISA frontend types
    implementation("org.eclipse.jdt:org.eclipse.jdt.core:3.41.0")

    // YAML parsing for v2 witnesses (violation_sequence and invariant_set)
    implementation("org.yaml:snakeyaml:2.2")

    // CLI argument parsing (consistent with jLISA's own CLI style)
    implementation("commons-cli:commons-cli:1.5.0")

    // Logging (consistent with jLISA)
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

// Make JDI types visible to the Java compiler.
// jdk.jdi is a JDK module whose packages (com.sun.jdi.*) are not in the default
// compile-time root set for unnamed-module projects.
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("--add-modules", "jdk.jdi"))
}

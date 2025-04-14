plugins {
    id("java")
}

group = "it.unive.jlisa"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("org.eclipse.jdt:org.eclipse.jdt.core:3.41.0")
    implementation("io.github.lisa-analyzer:lisa-sdk:0.1")

    implementation("io.github.lisa-analyzer:lisa-analyses:0.1")
    implementation("io.github.lisa-analyzer:lisa-program:0.1")
}

tasks.test {
    useJUnitPlatform()
}
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstyleExtension

// Apply Checkstyle plugin imperatively
apply<org.gradle.api.plugins.quality.CheckstylePlugin>()

// --------------------
// Checkstyle configuration
// --------------------
configure<CheckstyleExtension> {
    configFile = file("${rootProject.projectDir}/checkstyle-config.xml")
    sourceSets = emptyList()
    isShowViolations = true
    toolVersion = "8.38"
}

// Disable checkstyleTest
tasks.named<Checkstyle>("checkstyleTest") {
    isEnabled = false
}

// Configure checkstyleMain
tasks.named<Checkstyle>("checkstyleMain") {
    finalizedBy("checkstyleErrorMessage")
    reports {
        xml.required.set(false)
        html.required.set(false)
    }
}

// Task to show Checkstyle errors
tasks.register("checkstyleErrorMessage") {
    onlyIf {
        val checkstyleMain = tasks.named("checkstyleMain").get()
        checkstyleMain.state.failure != null
    }
    doLast {
        logger.error("Checkstyle plugin thrown an error. Javadoc may be incorrectly set up.")
        logger.error("To reproduce locally, execute './gradlew checkstyleMain'")
    }
}

// --------------------
// Combined code style check
// --------------------
tasks.register("checkCodeStyle") {
    group = "verification"
    description = "Execute Spotless and Checkstyle to ensure code and Javadoc formatting"
    dependsOn("spotlessCheck", "checkstyleMain", "checkstyleTest")
}

checkstyle {
    configFile = file(File(rootProject.projectDir, "checkstyle-config.xml"))
    sourceSets = emptyList()
    isShowViolations = true
    toolVersion = "8.38"
}

tasks.named<Checkstyle>("checkstyleTest") {
    isEnabled = false
}

tasks.named<Checkstyle>("checkstyleMain") {
    finalizedBy("checkstyleErrorMessage")
    reports {
        xml.required.set(false)
        html.required.set(false)
    }
}

tasks.register("checkstyleErrorMessage") {
    onlyIf {
        val checkstyleMain = tasks.named("checkstyleMain").get()
        checkstyleMain.state.failure != null
    }
    doLast {
        logger.error("Checkstyle plugin thrown an error. This means that the javadoc is not correctly setup. Inspect console output to find problematic javadocs.")
        logger.error("To reproduce locally, execute './gradlew checkstyleMain'")
    }
}

spotless {
    enforceCheck = false
    encoding = "UTF-8"
    lineEndings = com.diffplug.spotless.LineEnding.UNIX

    java {
        leadingSpacesToTabs()

        importOrder()
        removeUnusedImports()

        eclipse().configFile(File(rootProject.projectDir, "spotless-formatting.xml"))

        target("src/**/*.java")

        targetExclude("**/build/generated/**/*.java")
        targetExclude("**/build/generated-src/**/*.java")
        targetExclude("**/target/generated-sources/**/*.java")
        targetExclude("**/VersionInfo.java")
    }

    antlr4 {
        target("src/*/antlr/**/*.g4")
        antlr4Formatter()
    }
}

tasks.named("spotlessJava") {
    dependsOn("compileJava", "compileTestJava", "processTestResources", "spotlessAntlr4")
}

tasks.named("spotlessJavaCheck") {
    finalizedBy("spotlessErrorMessage")
}

tasks.named("spotlessAntlr4Check") {
    finalizedBy("spotlessErrorMessage")
}

tasks.register("spotlessErrorMessage") {
    onlyIf {
        val spotlessJavaCheck = tasks.named("spotlessJavaCheck").get()
        val spotlessAntlr4Check = tasks.named("spotlessAntlr4Check").get()
        spotlessJavaCheck.state.failure != null || spotlessAntlr4Check.state.failure != null
    }
    doLast {
        logger.error("Spotless plugin thrown an error. This means that the code is not correctly formatted.")
        logger.error("To reproduce locally, execute './gradlew spotlessCheck'")
        logger.error("To automatically fix all the problems, execute './gradlew spotlessApply'")
    }
}

tasks.register("checkCodeStyle") {
    group = "verification"
    description = "Execute spotless and checkstyle to ensure code and javadoc formatting"
    dependsOn("spotlessCheck", "checkstyleMain", "checkstyleTest")
}

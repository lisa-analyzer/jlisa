rootProject.name = "jlisa"

// builds against the local copy of lisa instead of the published snapshot
includeBuild("../../lisa/lisa") {
    dependencySubstitution {
        substitute(module("io.github.lisa-analyzer:lisa-sdk")).using(project(":lisa-sdk"))
        substitute(module("io.github.lisa-analyzer:lisa-analyses")).using(project(":lisa-analyses"))
        substitute(module("io.github.lisa-analyzer:lisa-program")).using(project(":lisa-program"))
    }
}

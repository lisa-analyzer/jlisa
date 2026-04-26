rootProject.name = "jlisa"

// Use the local lisa checkout (microservices branch) instead of the published
// artifact. The microservices branch is a v0.2 superset that adds the
// LiSAFrontend SPI plus the it.unive.lisa.analysis.network.* package; the
// published io.github.lisa-analyzer:*:0.2 is upstream master/v0.2 and lacks
// those. Group/version differ (it.unive:*:0.1 locally vs.
// io.github.lisa-analyzer:*:0.2 upstream), so the corresponding implementation
// declarations in build.gradle.kts have been switched to it.unive:*:0.1.
includeBuild("/Users/giacomo/02-work/03-microservices/lisa/lisa/lisa")


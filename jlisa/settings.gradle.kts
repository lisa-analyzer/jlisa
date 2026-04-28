rootProject.name = "jlisa"

// Use the local lisa checkout (microservices branch) instead of the published
// artifact. The microservices branch is a v0.2 superset that adds the
// LiSAFrontend SPI plus the it.unive.lisa.analysis.network.* package; the
// published io.github.lisa-analyzer:*:0.2 is upstream master/v0.2 and lacks
// those. Group/version differ (it.unive:*:0.1 locally vs.
// io.github.lisa-analyzer:*:0.2 upstream), so the corresponding implementation
// declarations in build.gradle.kts have been switched to it.unive:*:0.1.
//
// Relative includeBuild path: works on any host with the four repos arranged
// side-by-side under a common parent.
//   <parent>/lisa/lisa/lisa/
//   <parent>/jlisa/jlisa/                    (this repo)
//   <parent>/pylisa_microservices_lisa_upgrade/pylisa/
//   <parent>/lisa-network/lisa-network/lisa-network/
includeBuild("../../lisa/lisa/lisa")


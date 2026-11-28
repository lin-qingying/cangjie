import org.cangnova.gradle.tests.EnhanceTestFailureReportsTask

val extension = extensions.create("projectTests", ProjectTestsExtension::class, project)
val failureReportEnhancer = tasks.register<EnhanceTestFailureReportsTask>("enhanceTestFailureReports") {
    group = "verification"
    description = "Add colored line diffs to Gradle test failure reports"
    reportDirectory.set(layout.buildDirectory.dir("reports/tests"))
}

tasks.withType<Test>().configureEach {
    finalizedBy(failureReportEnhancer)

    val homeDir = project.ideaHomePathForTests().get()
    systemProperty("idea.home.path", homeDir.absolutePath)
    systemProperty("kotlin.test.update.test.data", project.findProperty("kotlin.test.update.test.data")?.toString() ?: "false")
    systemProperty("update.test.data", project.findProperty("update.test.data")?.toString() ?: "false")

    doFirst {
        val binDir = homeDir.resolve("bin")
        binDir.mkdirs()
        homeDir.resolve("build.txt").writeText("IC-999.SNAPSHOT")
        binDir.resolve("idea.properties").writeText("idea.config.path=")
    }
}

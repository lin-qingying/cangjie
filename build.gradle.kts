import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    base
    idea
    alias(libs.plugins.kover)
    alias(libs.plugins.kotlinJvm) apply false
    id("common-configuration") apply false
    id("cangjie-publishing") apply false
    id("analysis-coverage-convention") apply false
    id("project-tests-convention") apply false

 
}

val cangjieVersion = providers.gradleProperty("cangjieVersion").get()

allprojects {
    group = "org.cangnova.cangjie"
    version = cangjieVersion

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-Xskip-prerelease-check")
            freeCompilerArgs.add("-Xjvm-default=all")
            freeCompilerArgs.add("-XXLanguage:+ExplicitBackingFields")
            freeCompilerArgs.add("-Xcontext-parameters")
        }
    }

    pluginManager.apply("common-configuration")
}

dependencies {
    kover(project(":analysis:analysis-api-cfir"))
    kover(project(":analysis:low-level-api-cfir"))
    kover(project(":analysis:cj-references"))
    kover(project(":analysis:light-declarations"))
    kover(project(":analysis:symbol-light-declarations"))
    kover(project(":analysis:stubs"))
    kover(project(":analysis:decompiled"))
}

kover {
    reports {
        total {
            filters {
                excludes {
                    annotatedBy("*Generated*")
                }
            }
            html {
                onCheck = false
                title = "Analysis Coverage"
            }
            xml {
                onCheck = false
            }
        }
    }
}

tasks.register("reportAnalysisCoverage") {
    group = "verification"
    description = "Generate per-module and aggregated analysis coverage reports."
    dependsOn("koverHtmlReport", "koverXmlReport")
    dependsOn(
        ":analysis:analysis-api-cfir:koverHtmlReport",
        ":analysis:analysis-api-cfir:koverXmlReport",
        ":analysis:low-level-api-cfir:koverHtmlReport",
        ":analysis:low-level-api-cfir:koverXmlReport",
        ":analysis:cj-references:koverHtmlReport",
        ":analysis:cj-references:koverXmlReport",
        ":analysis:light-declarations:koverHtmlReport",
        ":analysis:light-declarations:koverXmlReport",
        ":analysis:symbol-light-declarations:koverHtmlReport",
        ":analysis:symbol-light-declarations:koverXmlReport",
        ":analysis:stubs:koverHtmlReport",
        ":analysis:stubs:koverXmlReport",
        ":analysis:decompiled:koverHtmlReport",
        ":analysis:decompiled:koverXmlReport",
    )
}

tasks.register("verifyAnalysisCoverage") {
    group = "verification"
    description = "Verify module-level analysis coverage thresholds."
    dependsOn(
        ":analysis:analysis-api-cfir:koverVerify",
        ":analysis:low-level-api-cfir:koverVerify",
        ":analysis:cj-references:koverVerify",
        ":analysis:light-declarations:koverVerify",
        ":analysis:symbol-light-declarations:koverVerify",
        ":analysis:stubs:koverVerify",
        ":analysis:decompiled:koverVerify",
    )
}

tasks.matching { it.name == "checkAnalysisFramework" }.configureEach {
    dependsOn("verifyAnalysisCoverage")
}

fun md(text: String): String = text.trimIndent()

val publicPublicationArtifacts = linkedMapOf(
    ":prepare:frontend" to ("cangjie-frontend" to md("""
        **Recommended for:** IntelliJ Platform / IDEA plugins and other **controlled-classpath** integrations.

        - Publishes the public Cangjie frontend runtime facade.
        - Keeps host dependency packages unchanged; **no relocation** is applied.
        - Suitable when the host process already provides compatible `com.intellij.*` and related runtime libraries.
        - Choose `cangjie-frontend-embeddable` instead when the host process is uncontrolled or may carry conflicting compiler / IDE dependencies.
    """)),
    ":prepare:frontend-embeddable" to ("cangjie-frontend-embeddable" to md("""
        **Recommended for:** embedding the Cangjie frontend into **host-uncontrolled** JVM processes.

        - Publishes the embeddable frontend runtime facade.
        - Applies **shading + relocation** to host-sensitive dependencies under `org.cangnova.cangjie.*`.
        - Designed to reduce classpath conflicts with IntelliJ, Guava, JDOM, FastUtil, and similar libraries already present in the host.
        - Not the preferred choice for ordinary IDEA plugins that need direct interop with the platform's original `com.intellij.*` packages.
    """)),
    ":prepare:test-infrastructure" to ("cangjie-frontend-test-infrastructure" to md("""
        **Recommended for:** compiler, parser, PSI, and integration tests built outside this repository.

        - Publishes the reusable Cangjie frontend test infrastructure facade.
        - Packages the shared `testFixtures` runtime needed to stand up frontend-oriented test environments.
        - Intended for consumers who want the repository's canonical test scaffolding without depending on internal module layout.
        - Works best together with the public frontend artifacts rather than direct internal project dependencies.
    """)),
    ":prepare:analysis-test-framework" to ("cangjie-frontend-analysis-test-framework" to md("""
        **Recommended for:** external tests targeting the public Analysis API contract.

        - Publishes the reusable Analysis API test framework facade.
        - Aggregates the shared `analysis-test-framework` and frontend test fixtures required by Analysis API test suites.
        - Useful for downstream projects that need the repository's standard analysis assertions, session setup, and fixture conventions.
        - Intended for test code and verification workflows, not for production runtime embedding.
    """)),
)

val idePublicationArtifacts = linkedMapOf(
    ":prepare:ide-plugin-dependencies:cangjie-frontend-common-for-ide" to ("cangjie-frontend-common-for-ide" to md("""
        **IDE plugin dependency:** packages :common, :util, :compiler:arguments, :resolution.common into a single fat jar.
    """)),
    ":prepare:ide-plugin-dependencies:cangjie-frontend-psi-for-ide" to ("cangjie-frontend-psi-for-ide" to md("""
        **IDE plugin dependency:** packages :psi (with :common, :util) into a single fat jar.
    """)),
    ":prepare:ide-plugin-dependencies:cangjie-frontend-cfir-for-ide" to ("cangjie-frontend-cfir-for-ide" to md("""
        **IDE plugin dependency:** packages :cfir:* full series and :common:diagnostics into a single fat jar.
    """)),
    ":prepare:ide-plugin-dependencies:cangjie-frontend-analysis-api-for-ide" to ("cangjie-frontend-analysis-api-for-ide" to md("""
        **IDE plugin dependency:** packages :analysis:analysis-api, :analysis:analysis-api-platform-interface, :analysis:analysis-api-impl-base into a single fat jar.
    """)),
    ":prepare:ide-plugin-dependencies:cangjie-frontend-analysis-api-cfir-for-ide" to ("cangjie-frontend-analysis-api-cfir-for-ide" to md("""
        **IDE plugin dependency:** packages :analysis:analysis-api-cfir, :analysis:low-level-api-cfir, :analysis:decompiled, :analysis:symbol-light-declarations into a single fat jar.
    """)),
    ":prepare:ide-plugin-dependencies:cangjie-frontend-analysis-api-standalone-for-ide" to ("cangjie-frontend-analysis-api-standalone-for-ide" to md("""
        **IDE plugin dependency:** packages :analysis:analysis-api-standalone, :analysis:analysis-internal-utils into a single fat jar.
    """)),
)

extensions.extraProperties["cangjiePublicProjectPaths"] = publicPublicationArtifacts.keys.toSet()

publicPublicationArtifacts.forEach { (projectPath, publication) ->
    val (artifactId, publicationDescription) = publication
    project(projectPath).run {
        extensions.extraProperties["cangjiePublicationArtifactId"] = artifactId
        extensions.extraProperties["cangjiePublicationDescription"] = publicationDescription
        pluginManager.apply("cangjie-publishing")
    }
}

idePublicationArtifacts.forEach { (projectPath, publication) ->
    val (artifactId, publicationDescription) = publication
    project(projectPath).run {
        extensions.extraProperties["cangjiePublicationArtifactId"] = artifactId
        extensions.extraProperties["cangjiePublicationDescription"] = publicationDescription
        pluginManager.apply("cangjie-publishing")
    }
}

val allPublicationArtifacts = publicPublicationArtifacts + idePublicationArtifacts

tasks.register("printPublicArtifactIds") {
    group = "publishing"
    description = "Print all public Maven artifactIds, one per line."
    doLast {
        allPublicationArtifacts.values
            .map { (artifactId, _) -> artifactId }
            .forEach(::println)
    }
}

tasks.register("publishPublicArtifacts") {
    group = "publishing"
    description = "Publish all public frontend and analysis Maven artifacts."
    dependsOn(allPublicationArtifacts.keys.map { "$it:publish" })
}

tasks.register("publish") {
    group = "publishing"
    description = "Publish all public frontend and analysis Maven artifacts."
    dependsOn("publishPublicArtifacts")
}

tasks.register("installPublicArtifacts") {
    group = "publishing"
    description = "Install all public frontend and analysis Maven artifacts to Maven Local."
    dependsOn(allPublicationArtifacts.keys.map { "$it:install" })
}

tasks.register("install") {
    group = "publishing"
    description = "Install all public frontend and analysis Maven artifacts to Maven Local."
    dependsOn("installPublicArtifacts")
}

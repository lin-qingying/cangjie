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

val publicPublicationArtifacts = linkedMapOf(
    ":prepare:frontend" to ("cangjie-frontend" to "Published Cangjie frontend runtime for IntelliJ Platform plugins and other controlled-classpath integrations. Uses original host dependency packages without relocation."),
    ":prepare:frontend-embeddable" to ("cangjie-frontend-embeddable" to "Embeddable Cangjie frontend runtime for host-uncontrolled integrations. Shades and relocates host-sensitive dependencies under org.cangnova.cangjie.* to avoid classpath conflicts."),
    ":prepare:test-infrastructure" to ("cangjie-frontend-test-infrastructure" to "Published Cangjie frontend compiler test infrastructure."),
    ":prepare:analysis-test-framework" to ("cangjie-frontend-analysis-test-framework" to "Published Cangjie frontend analysis API test framework."),
    ":compiler:arguments" to ("cangjie-frontend-arguments-description" to "Cangjie frontend argument model and generated argument descriptions."),
    ":common" to ("cangjie-frontend-common" to "Shared Cangjie frontend language model and core infrastructure."),
    ":psi" to ("cangjie-frontend-psi" to "Cangjie PSI, lexer, parser and source syntax infrastructure."),
    ":common:diagnostics" to ("cangjie-frontend-common-diagnostics" to "Cangjie frontend diagnostics model, factories, collectors and renderers."),
    ":analysis:analysis-api" to ("cangjie-frontend-analysis-api" to "Public Cangjie frontend analysis API."),
    ":analysis:analysis-api-platform-interface" to ("cangjie-frontend-analysis-api-platform-interface" to "Platform abstraction layer for the Cangjie frontend analysis API."),
    ":analysis:analysis-api-impl-base" to ("cangjie-frontend-analysis-api-impl-base" to "Base implementation layer for the Cangjie frontend analysis API."),
    ":analysis:analysis-api-standalone" to ("cangjie-frontend-analysis-api-standalone" to "Standalone entrypoints for consuming the Cangjie frontend analysis API."),
    ":analysis:analysis-api-cfir" to ("cangjie-frontend-analysis-api-cfir" to "CFIR-backed implementation of the Cangjie frontend analysis API."),
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

tasks.register("publishPublicArtifacts") {
    group = "publishing"
    description = "Publish all public frontend and analysis Maven artifacts."
    dependsOn(publicPublicationArtifacts.keys.map { "$it:publish" })
}

tasks.register("publish") {
    group = "publishing"
    description = "Publish all public frontend and analysis Maven artifacts."
    dependsOn("publishPublicArtifacts")
}

tasks.register("installPublicArtifacts") {
    group = "publishing"
    description = "Install all public frontend and analysis Maven artifacts to Maven Local."
    dependsOn(publicPublicationArtifacts.keys.map { "$it:install" })
}

tasks.register("install") {
    group = "publishing"
    description = "Install all public frontend and analysis Maven artifacts to Maven Local."
    dependsOn("installPublicArtifacts")
}

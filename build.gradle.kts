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
    ":compiler:arguments" to ("cangjie-frontend-arguments-description" to md("""
        **Recommended for:** tooling that needs a stable model of frontend compiler arguments.

        - Publishes the argument schema and generated argument description model used by the frontend toolchain.
        - Suitable for CLIs, IDE integrations, configuration UIs, and documentation generators that need to inspect supported options.
        - Keeps consumers off the internal argument-generation pipeline while exposing the public argument contract.
        - Useful when you need structured argument metadata rather than hard-coded flag tables.
    """)),
    ":common" to ("cangjie-frontend-common" to md("""
        **Recommended for:** libraries that need the public language model and foundational frontend abstractions.

        - Publishes shared core frontend infrastructure used across syntax, diagnostics, and analysis layers.
        - Contains the common domain model and stable base types that other public artifacts build upon.
        - A good starting dependency when you need frontend concepts without pulling in parser or analysis implementations.
        - Prefer this artifact over internal module dependencies when building against the published ecosystem.
    """)),
    ":psi" to ("cangjie-frontend-psi" to md("""
        **Recommended for:** syntax-aware tooling, source parsing, and PSI-based integrations.

        - Publishes Cangjie PSI, lexer, parser, and source syntax infrastructure.
        - Suitable for editor tooling, inspections, indexing experiments, and syntax tree traversal use cases.
        - Exposes the public syntax layer without requiring consumers to depend on repository-internal PSI packaging details.
        - Often paired with `cangjie-frontend-common` and diagnostics or analysis artifacts.
    """)),
    ":common:diagnostics" to ("cangjie-frontend-common-diagnostics" to md("""
        **Recommended for:** tooling that needs structured frontend diagnostics rather than plain text errors.

        - Publishes the diagnostics model, factories, collectors, severities, and renderers shared by the frontend stack.
        - Suitable for IDE reporting, CLI rendering, verification frameworks, and custom diagnostic pipelines.
        - Lets downstream consumers interpret frontend failures through typed diagnostic objects instead of ad-hoc strings.
        - Commonly used together with PSI and Analysis API artifacts.
    """)),
    ":analysis:analysis-api" to ("cangjie-frontend-analysis-api" to md("""
        **Recommended for:** consumers that want the public semantic analysis contract.

        - Publishes the stable, public Analysis API surface of the Cangjie frontend.
        - Exposes semantic queries and analysis-facing abstractions without binding consumers to a concrete backend implementation.
        - Intended as the main entry dependency for semantic tooling that should remain implementation-agnostic.
        - Pair with a backend implementation such as `cangjie-frontend-analysis-api-cfir` when you need an executable engine.
    """)),
    ":analysis:analysis-api-platform-interface" to ("cangjie-frontend-analysis-api-platform-interface" to md("""
        **Recommended for:** hosts that must bridge platform services into the Analysis API.

        - Publishes the platform abstraction layer used by the public Analysis API.
        - Defines the contracts through which the analysis stack integrates with host-specific platform capabilities.
        - Useful for custom environments that need to adapt filesystem, project-model, or platform services without exposing internal implementations.
        - Typically consumed together with `cangjie-frontend-analysis-api` and a concrete backend or standalone entrypoint.
    """)),
    ":analysis:analysis-api-impl-base" to ("cangjie-frontend-analysis-api-impl-base" to md("""
        **Recommended for:** advanced integrators building custom Analysis API implementations or extensions.

        - Publishes the base implementation layer shared by concrete Analysis API backends.
        - Provides reusable implementation scaffolding while keeping higher-level consumers on the public API surface.
        - Useful when you need to extend, adapt, or host a backend implementation rather than merely call semantic queries.
        - Most ordinary consumers should start from `cangjie-frontend-analysis-api` or `cangjie-frontend-analysis-api-standalone` instead.
    """)),
    ":analysis:analysis-api-standalone" to ("cangjie-frontend-analysis-api-standalone" to md("""
        **Recommended for:** standalone tools that want a ready-to-use Analysis API entrypoint.

        - Publishes the standalone bootstrap layer for consuming the public Analysis API outside repository-internal runtime wiring.
        - Bridges the public API, platform abstraction layer, and implementation base into a downstream-friendly entry surface.
        - Suitable for analyzers, CLIs, batch tooling, and experiments that need semantic services without recreating the repository's bootstrapping code.
        - Prefer this artifact when you want executable analysis capabilities with minimal host-specific plumbing.
    """)),
    ":analysis:analysis-api-cfir" to ("cangjie-frontend-analysis-api-cfir" to md("""
        **Recommended for:** consumers that explicitly want the **CFIR-backed** Analysis API implementation.

        - Publishes the concrete CFIR backend for the public Analysis API.
        - Connects semantic queries to the repository's CFIR-based analysis engine and related implementation layers.
        - Use this artifact when you need the actual CFIR execution backend rather than only the abstract Analysis API contract.
        - Typically consumed behind the public API surface, but available directly for advanced integrations that need CFIR-specific behavior.
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

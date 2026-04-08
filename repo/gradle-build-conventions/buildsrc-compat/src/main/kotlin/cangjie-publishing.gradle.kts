import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.GenerateMavenPom
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType

plugins {
    `maven-publish`
}

val publicationArtifactId = providers.provider {
    (findProperty("cangjiePublicationArtifactId") as? String)?.takeIf { it.isNotBlank() } ?: name
}

val publicationDescription = providers.provider {
    (findProperty("cangjiePublicationDescription") as? String)?.takeIf { it.isNotBlank() }
        ?: (project.description?.toString()?.takeIf { it.isNotBlank() })
        ?: publicationArtifactId.get()
}

val publicProjectPaths = ((rootProject.extensions.extraProperties.properties["cangjiePublicProjectPaths"] as? Set<*>) ?: emptySet<Any>())
    .filterIsInstance<String>()
    .toSet()

fun publishedArtifactId(projectPath: String): String {
    val dependencyProject = project(projectPath)
    return (dependencyProject.findProperty("cangjiePublicationArtifactId") as? String)?.takeIf { it.isNotBlank() }
        ?: dependencyProject.name
}

fun unpublishedProjectDependencies(): List<String> {
    val runtimeClasspath = configurations.findByName("runtimeClasspath") ?: return emptyList()
    return runtimeClasspath
        .incoming
        .resolutionResult
        .allComponents
        .mapNotNull { component -> (component.id as? ProjectComponentIdentifier)?.projectPath }
        .filter { projectPath -> projectPath != path && projectPath !in publicProjectPaths }
        .distinct()
}

plugins.withId("java") {
    val javaExtension = javaPluginExtension()
    javaExtension.withSourcesJar()
    javaExtension.withJavadocJar()

    tasks.withType<Javadoc>().configureEach {
        options.encoding = "UTF-8"
        isFailOnError = false
    }

    /**
     * 当前公开发布的是“前端门面工件”：
     * 这些工件会把未公开的一方模块直接打进最终 jar，只保留清洗后的 Maven POM 依赖图。
     *
     * 如果继续发布 Gradle Module Metadata，Gradle 消费端会优先读取 `.module`，
     * 从而重新看到 `cfir-tree`、`resolve`、`checkers` 等内部实现依赖，导致消费方解析失败。
     *
     * 因此这里对公开门面工件统一关闭 `.module` 生成，让 Gradle 回退到已清洗的 POM。
     */
    tasks.withType<GenerateModuleMetadata>().configureEach {
        enabled = false
    }
    tasks.matching { it.name == "generateMetadataFileForMavenPublication" }.configureEach {
        enabled = false
    }

    afterEvaluate {
        val publicationComponentName = if (pluginManager.hasPlugin("com.gradleup.shadow")) "shadow" else "java"
        val publishesShadowComponent = publicationComponentName == "shadow"

        if (!publishesShadowComponent) {
            tasks.named<Jar>("jar").configure {
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                dependsOn(unpublishedProjectDependencies().map { dependencyPath ->
                    project(dependencyPath).tasks.named("jar")
                })
                from({
                    unpublishedProjectDependencies().map { dependencyPath ->
                        zipTree(project(dependencyPath).tasks.named<Jar>("jar").get().archiveFile.get().asFile)
                    }
                })
            }
        }

        configure<PublishingExtension> {
            publications {
                val publication = (findByName("maven") as? MavenPublication) ?: create<MavenPublication>("maven")
                publication.apply {
                    artifactId = publicationArtifactId.get()
                    from(components.getByName(publicationComponentName))

                    if (publishesShadowComponent) {
                        artifact(tasks.named<Jar>("sourcesJar"))
                        artifact(tasks.named<Jar>("javadocJar"))
                    }

                    pom {
                        name.set(providers.provider { publicationArtifactId.get() })
                        description.set(providers.provider { publicationDescription.get() })
                        url.set("https://github.com/cangnova/cangjie")
                        licenses {
                            license {
                                name.set("Apache-2.0")
                                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                            }
                        }
                        scm {
                            url.set("https://github.com/cangnova/cangjie")
                            connection.set("scm:git:https://github.com/cangnova/cangjie.git")
                            developerConnection.set("scm:git:https://github.com/cangnova/cangjie.git")
                        }
                        developers {
                            developer {
                                name.set("Cangjie Frontend Team")
                                organization.set("Cangnova")
                                organizationUrl.set("https://github.com/cangnova")
                            }
                        }
                    }
                }
            }

            repositories {
                maven {
                    name = (findProperty("cangjie.build.deploy-repo") as? String)?.ifBlank { null } ?: "cangjie"

                    val deployUrl = (findProperty("cangjie.build.deploy-url") as? String)?.ifBlank { null }
                    val deployPath = (findProperty("cangjie.build.deploy-path") as? String)?.ifBlank { null }
                    val repoUrl = deployUrl
                        ?: deployPath?.let { rootProject.layout.projectDirectory.dir(it).asFile.toURI().toString() }
                        ?: rootProject.layout.buildDirectory.dir("repo").get().asFile.toURI().toString()
                    setUrl(repoUrl)

                    val username = (findProperty("cangjie.build.deploy-username") as? String)?.ifBlank { null }
                    val password = (findProperty("cangjie.build.deploy-password") as? String)?.ifBlank { null }
                    if (url.scheme != "file" && username != null && password != null) {
                        credentials {
                            this.username = username
                            this.password = password
                        }
                    }
                }
            }
        }

        val projectGroupId = project.group.toString()
        val internalArtifactIds = unpublishedProjectDependencies()
            .map(::publishedArtifactId)
            .toSet()

        tasks.withType<GenerateMavenPom>().configureEach {
            doLast {
                if (internalArtifactIds.isEmpty()) return@doLast

                val pomFile = destination
                var pomText = pomFile.readText(Charsets.UTF_8)
                internalArtifactIds.forEach { internalArtifactId ->
                    val dependencyPattern = Regex(
                        """\s*<dependency>\s*<groupId>${Regex.escape(projectGroupId)}</groupId>\s*<artifactId>${Regex.escape(internalArtifactId)}</artifactId>.*?</dependency>""",
                        setOf(RegexOption.DOT_MATCHES_ALL),
                    )
                    pomText = pomText.replace(dependencyPattern, "")
                }
                pomFile.writeText(pomText, Charsets.UTF_8)
            }
        }
    }

    val signingRequired = providers.gradleProperty("cangjie.build.signing-required")
        .map(String::toBoolean)
        .orElse(false)
        .get()

    if (signingRequired) {
        pluginManager.apply(SigningPlugin::class.java)
        configure<SigningExtension> {
            val signKeyId = (findProperty("signKeyId") as? String)?.ifBlank { null } ?: System.getenv("signKeyId")
            if (signKeyId != null) {
                val signKeyPrivate = (findProperty("signKeyPrivate") as? String)?.ifBlank { null }
                    ?: System.getenv("signKeyPrivate")
                    ?: error("Missing signKeyPrivate")
                val signKeyPassphrase = (findProperty("signKeyPassphrase") as? String)?.ifBlank { null }
                    ?: System.getenv("signKeyPassphrase")
                    ?: error("Missing signKeyPassphrase")
                useInMemoryPgpKeys(signKeyId, signKeyPrivate, signKeyPassphrase)
            } else {
                useGpgCmd()
            }
            sign(extensions.getByType<PublishingExtension>().publications)
        }

        tasks.withType<AbstractPublishToMaven>().configureEach {
            mustRunAfter(tasks.withType<Sign>())
        }
    }

    tasks.register("install") {
        group = "publishing"
        description = "Publish this module to Maven Local."
        dependsOn(tasks.named("publishToMavenLocal"))
    }
}

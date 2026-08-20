import org.gradle.api.GradleException
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import java.io.File

plugins {
    id("java")
    id("org.jetbrains.intellij.platform")
}

val devEcoHome = providers.environmentVariable("DEVECO_HOME")
    .orElse(providers.gradleProperty("devEcoHome"))
val syncPlatformVersion = providers.gradleProperty("devecoSyncPlatformVersion")
val bundledCangjiePluginId = providers.gradleProperty("devecoBundledCangjiePluginId")
val cangjieIdeVersion = providers.gradleProperty("cangjieIdeVersion")
val cangjieRootDir = rootProject.layout.projectDirectory.asFile.parentFile
val officialCangjieRuntimeDir = rootProject.layout.projectDirectory.dir("lib/plugin/cangjie/lib")
val officialCangjieRuntimeJars = fileTree(officialCangjieRuntimeDir.asFile) {
    include("*.jar")
}
val officialCangjieRuntimeResourceIncludes = listOf(
    "templates/**",
    "cjlint/**",
)
val requiredOfficialCangjieJars = listOf(
    "Cangjie-Support-Plugin-5.0.13.200.jar",
    "Cangjie-deveco-plugins-5.0.13.200.jar",
    "Cangjie-project-mgmt-5.0.13.200.jar",
    "Cangjie-SDK-Manager-5.0.13.200.jar",
    "Cangjie-dap-client-5.0.13.200.jar",
    "cjformat-5.0.13.200.jar",
    "cjlint-5.0.13.200.jar",
)
val officialCangjieRuntimePath = officialCangjieRuntimeDir.asFile.absolutePath
val requiredOfficialCangjieJarNames = requiredOfficialCangjieJars.toList()
val requiredOfficialCangjieDirNames = listOf("templates", "cjlint")
val cangjieFrontendRuntimeProjectPaths = listOf(
    ":common",
    ":util",
    ":analysis:cj-references",
    ":compiler:arguments",
    ":resolution.common",
    ":psi",
    ":code-insight:api",
    ":code-insight:fixes",
    ":code-insight:override-implement",
    ":code-insight:formatting",
    ":code-insight:folding",
    ":code-insight:highlighting",
    ":code-insight:refactoring",
    ":analysis:analysis-api",
    ":analysis:analysis-api-platform-interface",
    ":analysis:analysis-api-impl-base",
    ":analysis:analysis-api-cfir",
    ":analysis:analysis-api-standalone",
    ":analysis:analysis-internal-utils",
    ":analysis:low-level-api-cfir",
    ":analysis:decompiled",
    ":analysis:decompiled:decompiler-to-file-stubs",
    ":analysis:decompiled:decompiler-to-stubs",
    ":analysis:decompiled:decompiler-to-psi",
    ":analysis:decompiled:light-declarations-for-decompiled",
    ":analysis:stubs",
    ":analysis:symbol-light-declarations",
    ":cfir:cfir-common",
    ":cfir:cfir-cones",
    ":cfir:cfir-tree",
    ":cfir:cfir-serialization",
    ":cfir:providers",
    ":cfir:resolve",
    ":cfir:semantics",
    ":compiler:config",
    ":cfir:checkers",
    ":cfir:diagnostic-renderers",
    ":cfir:raw-cfir:raw-cfir-common",
    ":cfir:raw-cfir:psi2cfir",
    ":cfir:raw-cfir:light-tree2cfir",
    ":cfir:entrypoint",
    ":common:diagnostics",
).distinct()
val cangjieFrontendRuntimeJarPaths = cangjieFrontendRuntimeProjectPaths.map(::cangjieRuntimeJarPath)
val cangjieFrontendRuntimeJars = files(cangjieFrontendRuntimeJarPaths)

fun cangjieRuntimeJarPath(projectPath: String): String {
    val pathSegments = projectPath.removePrefix(":").split(":")
    val jarBaseName = pathSegments.last()
    return File(
        cangjieRootDir,
        pathSegments.joinToString(File.separator) + File.separator + "build" + File.separator +
            "libs" + File.separator + "$jarBaseName-${cangjieIdeVersion.get()}.jar",
    ).absolutePath
}

val checkOfficialCangjieRuntime by tasks.registering {
    inputs.dir(officialCangjieRuntimeDir)
    inputs.property("runtimePath", officialCangjieRuntimePath)
    inputs.property("requiredJarNames", requiredOfficialCangjieJarNames)
    inputs.property("requiredDirNames", requiredOfficialCangjieDirNames)

    doLast {
        val runtimeDir = File(inputs.properties["runtimePath"] as String)
        val jarNames = inputs.properties["requiredJarNames"] as Iterable<*>
        val dirNames = inputs.properties["requiredDirNames"] as Iterable<*>
        val missingFiles = jarNames
            .map { File(runtimeDir, it.toString()) }
            .filter { !it.isFile }

        val missingDirs = dirNames
            .map { File(runtimeDir, it.toString()) }
            .filter { !it.isDirectory }

        if (missingFiles.isNotEmpty() || missingDirs.isNotEmpty()) {
            val missing = (missingFiles + missingDirs).joinToString(separator = "\n") { " - ${it.absolutePath}" }
            throw GradleException("DevEco Cangjie 官方运行时资源不完整：\n$missing")
        }
    }
}

val checkCangjieFrontendRuntime by tasks.registering {
    inputs.files(cangjieFrontendRuntimeJars)
    inputs.property("jarPaths", cangjieFrontendRuntimeJarPaths)

    doLast {
        val missingJars = (inputs.properties["jarPaths"] as Iterable<*>)
            .map { File(it.toString()) }
            .filter { !it.isFile }

        if (missingJars.isNotEmpty()) {
            throw GradleException(
                "仓颉前端本地运行时 jar 不完整，请先在主仓库构建对应模块：\n" +
                    missingJars.joinToString(separator = "\n") { " - ${it.absolutePath}" }
            )
        }
    }
}

val checkPluginRuntimeClasspath by tasks.registering {
    val runtimeClasspath = configurations.named("runtimeClasspath")
    inputs.files(runtimeClasspath)

    doLast {
        val forbiddenPlatformJar = Regex(
            """^(util|util-base|util-class-loader|util-rt|util-xml-dom|core|ide-impl|lang|analysis|indexing|project-model|extensions|plugins-parser-impl)-(?:\d{3}\.|[A-Z]{2}-).+\.jar$"""
        )
        val offenders = inputs.files.files
            .map(File::getName)
            .filter(forbiddenPlatformJar::matches)
            .sorted()

        if (offenders.isNotEmpty()) {
            throw GradleException(
                "插件运行时 classpath 不能打入 IntelliJ Platform jar：\n" +
                    offenders.joinToString(separator = "\n") { " - $it" }
            )
        }
    }
}

dependencies {
    implementation(project(":modules:core"))
    implementation(project(":modules:deveco-bridge"))

    runtimeOnly(officialCangjieRuntimeJars)
    runtimeOnly(cangjieFrontendRuntimeJars)
    runtimeOnly(libs.bundles.jackson)
    runtimeOnly(libs.toml4j)
    runtimeOnly(libs.jansi)
    runtimeOnly(libs.vavr)
    runtimeOnly(libs.lsp4j.debug)
    runtimeOnly(libs.bundles.protobuf)
    runtimeOnly(libs.okio)

    intellijPlatform {
        if (devEcoHome.isPresent) {
            local(file(devEcoHome.get()))
            if (bundledCangjiePluginId.isPresent) {
                bundledPlugin(bundledCangjiePluginId.get())
            }
        } else {
            intellijIdea(syncPlatformVersion.get())
        }
        testFramework(TestFrameworkType.Platform)
    }
}

intellijPlatform {
    autoReload = true

    pluginConfiguration {
        name = "Cangjie DevEco"

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = provider { null }
        }
    }
}

tasks {
    // DevEco 扩展点依赖真实 DevEco 宿主；fallback IDEA 只用于产物构建，不生成 searchable options。
    named("buildSearchableOptions") {
        enabled = devEcoHome.isPresent
    }
    named("prepareJarSearchableOptions") {
        enabled = devEcoHome.isPresent
    }
    named("jarSearchableOptions") {
        enabled = devEcoHome.isPresent
    }

    buildPlugin {
        dependsOn(checkOfficialCangjieRuntime)
        dependsOn(checkCangjieFrontendRuntime)
        dependsOn(checkPluginRuntimeClasspath)
        archiveBaseName.set("cangjie-deveco")
        from(officialCangjieRuntimeDir) {
            officialCangjieRuntimeResourceIncludes.forEach(::include)
            into("lib")
        }
    }

    runIde {
        if (!devEcoHome.isPresent) {
            enabled = false
        }
        doFirst {
            if (!devEcoHome.isPresent) {
                throw GradleException("runIde 需要先配置 DEVECO_HOME 或 gradle.properties 中的 devEcoHome。")
            }
        }
    }
}

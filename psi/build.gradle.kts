/*
 * PSI 模块：仓颉语言的程序结构接口。
 *
 * IntelliJ Platform 依赖通过聚合模块 :dependencies:intellij-core 统一管理，
 * 对齐 Kotlin K2 编译器的 compileOnly(intellijCore()) 模式。
 *
 * Lexer 生成参考 Kotlin 的 jflexPath 模式，不使用 GrammarKit 插件。
 *
 * 语法 / 词法测试采用 testData 驱动：testFixtures 提供词法与解析测试基类，
 * TestGeneratorForPsi 扫描 psi/testData 下的 .cj 文件生成 JUnit5 测试入口
 * （tests-gen 目录），与 psi2cfir / analysis-tests 的生成式测试保持一致。
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
}

description = "Cangjie PSI, lexer, parser and source syntax infrastructure."

val jflexPath by configurations.creating

val intellijSdkVersion = property("intellijSdkVersion") as String

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(testFixtures(project(":tests:test-infrastructure")))
    testRuntimeOnly(libs.junit.platform.launcher)
    // TestDataPath 注解仅编译期使用（IDE 测试数据导航），运行期不需要 test-framework。
    testCompileOnly(intellijTestFramework()) { isTransitive = false }

    // CDoc/Quick Doc 渲染测试需要完整平台：MockApplication（core-impl 注册服务）
    // 与 EditorColorsScheme 实现（editor-ex，intellij-core 聚合里没有 editor 系列模块）。
    testImplementation(intellijCore())
    testImplementation("com.jetbrains.intellij.platform:editor-ex:$intellijSdkVersion") { isTransitive = false }
    testImplementation("com.jetbrains.intellij.platform:editor-ui:$intellijSdkVersion") { isTransitive = false }

    // 词法 / 解析测试基类与 TestGeneratorForPsi 位于本模块 testFixtures。
    testFixturesApi(testFixtures(project(":tests:test-infrastructure")))

    implementation(project(":util"))
    implementation(project(":common"))

    compileOnly(intellijCore())
    // CDoc 正文渲染与 Kotlin KDocRenderer 同构：直接用 JetBrains 官方 Markdown 解析器，
    // 不手写 Markdown 子集。IDE 运行时由平台提供同名包，故仅编译期 + 测试期引入。
    compileOnly(libs.markdown)
    testImplementation(libs.markdown)
    implementation(libs.guava)

    jflexPath("org.jetbrains.intellij.deps.jflex:jflex:1.10.14") {
        isTransitive = false
    }
}

// Lexer 生成任务（对齐 Kotlin 的 jflexPath 模式）
fun createLexerTask(taskName: String, flexFile: String, outputDir: String) =
    tasks.register<JavaExec>(taskName) {
        mainClass.set("jflex.Main")
        classpath = jflexPath
        args = listOf(
            file(flexFile).absolutePath,
            "-d",
            file(outputDir).absolutePath,
            "--nobak",
        )
        inputs.file(flexFile)
        outputs.dir(outputDir)
    }

val generateCangJieLexer = createLexerTask(
    "generateCangJieLexer",
    "src/org/cangnova/cangjie/lexer/CangJieLexer.flex",
    "gen/org/cangnova/cangjie/lexer",
)

val generateCDocLexer = createLexerTask(
    "generateCDocLexer",
    "src/org/cangnova/cangjie/lexer/cdoc/lexer/CDoc.flex",
    "gen/org/cangnova/cangjie/lexer/cdoc/lexer",
)

tasks.register("generateLexers") {
    dependsOn(generateCangJieLexer, generateCDocLexer)
    group = "build"
    description = "Generate all lexers for the project"
}

tasks.compileJava {
    dependsOn("generateLexers")
    dependsOn(tasks.compileKotlin)
    classpath += files(tasks.compileKotlin.map { it.destinationDirectory })
}

tasks.compileKotlin {
    dependsOn("generateLexers")
}

tasks.matching { it.name == "kotlinSourcesJar" || it.name == "sourcesJar" }.configureEach {
    dependsOn("generateLexers")
}

sourceSets {
    "main" {
        projectDefault()
        generatedDir()
    }
    "test" {
        projectDefault()
        generatedTestDir()
    }
    "testFixtures" { projectDefault() }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    freeCompilerArgs.add("-Xcontext-parameters")
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        workingDir = rootDir
        val updateTestData = System.getProperty("update.test.data")
        if (updateTestData != null) {
            systemProperty("update.test.data", updateTestData)
        }
        // testData 是测试任务的运行时输入：.cj / .cj.d / .cdoc 数据或 golden 变更必须触发重跑，
        // 否则编辑测试数据后会沿用上一次的通过结果。
        inputs.dir("testData").withPropertyName("psiTestData")
    }

    testGenerator("org.cangnova.cangjie.psi.test.TestGeneratorForPsi")
}

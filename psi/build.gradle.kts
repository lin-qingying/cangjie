/*
 * PSI 模块：仓颉语言的程序结构接口。
 *
 * IntelliJ Platform 依赖通过聚合模块 :dependencies:intellij-core 统一管理，
 * 对齐 Kotlin K2 编译器的 compileOnly(intellijCore()) 模式。
 *
 * Lexer 生成参考 Kotlin 的 jflexPath 模式，不使用 GrammarKit 插件。
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val jflexPath by configurations.creating

dependencies {
    testImplementation(kotlin("test"))

    implementation(project(":util"))
    implementation(project(":common"))

    compileOnly(intellijCore())
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

sourceSets {
    "main" {
        projectDefault()
        generatedDir()
    }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-Xcontext-parameters"))
}

plugins {
    kotlin("jvm")
    id("native-compile-plugin")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

dependencies {
    implementation(project(":llvm-interop:llvm-interop-api"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

val generateJniHeaders by tasks.registering(JavaCompile::class) {
    group = "build"
    description = "Generate JNI headers from Kotlin external declarations."
    source = sourceSets["main"].java
    classpath = sourceSets["main"].compileClasspath
    destinationDirectory.set(layout.buildDirectory.dir("tmp/jni-header-classes"))
    options.compilerArgs.addAll(
        listOf(
            "-h",
            layout.buildDirectory.dir("generated/jni-headers").get().asFile.absolutePath,
        ),
    )
}

nativeCompile {
    sourceDir("src/main/native")
    includeDir(layout.buildDirectory.dir("generated/jni-headers"))
    compilerArgs.add("-std=c++17")
    outputName.set("cangjie_llvm_jni")
}

tasks.named("nativeCompile") {
    dependsOn(generateJniHeaders)
}

val currentPlatformId = run {
    val os = System.getProperty("os.name").lowercase()
    val archRaw = System.getProperty("os.arch").lowercase()
    val arch = when (archRaw) {
        "amd64", "x86_64" -> "x86_64"
        "aarch64", "arm64" -> "aarch64"
        else -> archRaw
    }
    val normalizedOs = when {
        os.contains("win") -> "windows"
        os.contains("mac") -> "macos"
        os.contains("linux") -> "linux"
        else -> "unknown"
    }
    "$normalizedOs-$arch"
}

val nativeLibraryFileName = run {
    val os = System.getProperty("os.name").lowercase()
    when {
        os.contains("win") -> "cangjie_llvm_jni.dll"
        os.contains("mac") -> "libcangjie_llvm_jni.dylib"
        else -> "libcangjie_llvm_jni.so"
    }
}

val aggregateNativeArtifacts by tasks.registering(Copy::class) {
    group = "build"
    description = "Copies native JNI library into native/<os>-<arch>/ layout for packaging."
    dependsOn("nativeCompile")

    val builtLibrary = layout.buildDirectory.file("native/$nativeLibraryFileName")
    from(builtLibrary)
    into(layout.buildDirectory.dir("native/$currentPlatformId"))

    // nativeCompile can skip when LLVM is missing.
    onlyIf { builtLibrary.get().asFile.exists() }
}

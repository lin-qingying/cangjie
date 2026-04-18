plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("org.jetbrains.kotlin.jvm")
}

group = "org.cangnova.cangjie.build"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

kotlin {
    jvmToolchain(21)
}

gradlePlugin {
    plugins {
        create("nativeCompilePlugin") {
            id = "native-compile-plugin"
            implementationClass = "org.cangnova.buildtools.nativecompile.NativeCompilePlugin"
        }
    }
}

plugins {
    kotlin("jvm")
}

description = "IDE 插件依赖：打包 :analysis:analysis-api-cfir、:analysis:low-level-api-cfir、:analysis:decompiled、:analysis:symbol-light-declarations。"
configurations.configureEach {
    exclude(group = "com.jetbrains.intellij.platform")
    exclude(group = "com.jetbrains.intellij")
    exclude(group = "org.jetbrains.intellij")
    exclude(group = "com.intellij.platform")
}

dependencies {
    api(project(":analysis:analysis-api-cfir"))
    api(project(":analysis:low-level-api-cfir"))
    api(project(":analysis:decompiled"))
    api(project(":analysis:decompiled:decompiler-to-file-stubs"))
    api(project(":analysis:decompiled:decompiler-to-stubs"))
    api(project(":analysis:decompiled:decompiler-to-psi"))
    api(project(":analysis:decompiled:light-declarations-for-decompiled"))
    api(project(":analysis:stubs"))
    api(project(":analysis:symbol-light-declarations"))
}

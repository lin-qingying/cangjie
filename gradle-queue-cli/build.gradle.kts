/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

plugins {
    kotlin("jvm")
    application
}

// 注意：本模块刻意不使用 common-configuration 等项目约定插件，
// 保持零内部依赖，避免首次构建 queue-cli 时触发大量上游模块编译。

group = "org.cangnova.build"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}

application {
    mainClass.set("org.cangnova.build.queue.GradleQueueMainKt")
}

tasks.jar {
    archiveBaseName.set("gradle-queue-cli")
    archiveClassifier.set("plain")
    manifest {
        attributes("Main-Class" to "org.cangnova.build.queue.GradleQueueMainKt")
    }
}

/** fat jar: 包含 kotlin-stdlib，输出到 build/libs/gradle-queue-cli.jar。 */
val shadowJar = tasks.register<Jar>("shadowJar") {
    group = "build"
    description =
        "Builds a self-contained executable fat jar (kotlin-stdlib bundled) for gradle-queue."
    archiveBaseName.set("gradle-queue-cli")
    archiveClassifier.set("")
    archiveVersion.set("")
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes(
            "Main-Class" to "org.cangnova.build.queue.GradleQueueMainKt",
            "Implementation-Version" to project.version.toString(),
        )
    }

    // 把 main runtimeClasspath（包含 kotlin-stdlib jar + classes 输出目录）展开打进 jar。
    // 通过 provider {} 在任务执行阶段再解析 configuration，避免配置期 resolution-order 问题。
    // 同时 filter exists 去掉不存在的 java 输出目录占位，避免 zipTree(Cannot expand ZIP ... as it does not exist)。
    from(
        provider {
            configurations.runtimeClasspath.get()
                .filter { it.exists() }
                .map { f ->
                    if (f.isDirectory) files(f)
                    else zipTree(f)
                }
        },
    )
    with(tasks.jar.get())
    exclude(
        "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA",
        "META-INF/versions/*/module-info.class",
    )
}

tasks.named("assemble") {
    dependsOn(shadowJar)
}

kotlin {
    jvmToolchain(17)
}

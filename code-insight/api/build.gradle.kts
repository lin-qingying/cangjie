plugins {
    kotlin("jvm")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

/*
 * 契约层：只允许声明服务、数据类型与扩展点，不允许出现实现。
 *
 * 这里的类型会引用 Analysis API（例如成员生成以 CaSession/CaSymbol 为参数），
 * 因此 `api` 依赖 Analysis API；但**语法层 `syntax` 不允许依赖本模块**，
 * 从而保证「不需要 resolve 的能力」在编译期就拿不到 Analysis 类型。
 */
dependencies {
    compileOnly(intellijCore())

    implementation(project(":analysis:analysis-api"))
    implementation(project(":psi"))
    // 补全契约直接承载仓颉的 ImportPath（含组织名与别名），不另造平行字符串结构。
    implementation(project(":common"))

    testImplementation(intellijCore())
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

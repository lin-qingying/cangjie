plugins {
    kotlin("jvm")
    id("java-test-fixtures")
}

sourceSets {
    "main" { none() }
    "test" { none() }
    "testFixtures" { projectDefault() }
}

dependencies {

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testFixturesApi(project(":compiler:cli"))
    testFixturesApi(project(":compiler:config"))

    testFixturesApi(project(":psi"))
    testFixturesApi(intellijCore())
    testFixturesApi(libs.junit4)
    // TestDataPath 注解仅编译期使用，compileOnly 避免运行时加载 JUnit5TestSessionListener
    testFixturesCompileOnly(intellijTestFramework()) { isTransitive = false }
}

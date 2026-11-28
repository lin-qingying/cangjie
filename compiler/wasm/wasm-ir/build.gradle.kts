plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:wasm:wasm-common"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

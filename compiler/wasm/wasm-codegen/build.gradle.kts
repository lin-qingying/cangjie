plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:wasm:wasm-common"))
    api(project(":compiler:wasm:wasm-ir"))
    api(project(":chir:chir-tree"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

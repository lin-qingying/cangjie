plugins {
    kotlin("jvm")
}
sourceSets {
    "main" {
        projectDefault()
        generatedDir()
    }
    "test" { none() }
}

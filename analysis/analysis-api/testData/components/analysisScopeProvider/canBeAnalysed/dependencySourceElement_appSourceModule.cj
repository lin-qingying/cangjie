// MODULE: dependency
// MODULE_KIND: Source
// MAIN_MODULE
// FILE: dependency.cj
package sample.analysis.scope.dependency

func pr<caret>ovide(): Int64 {
    return 1
}

// MODULE: app(dependency)
// MODULE_KIND: Source
// USE_SITE_MODULE
// FILE: app.cj
package sample.analysis.scope.app

func consume(): Int64 {
    return provide()
}

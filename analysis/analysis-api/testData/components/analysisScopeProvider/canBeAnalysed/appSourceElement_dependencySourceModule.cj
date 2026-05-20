// MODULE: dependency
// MODULE_KIND: Source
// USE_SITE_MODULE
// FILE: dependency.cj
package sample.analysis.scope.dependency

func provide(): Int64 {
    return 1
}

// MODULE: app(dependency)
// MODULE_KIND: Source
// MAIN_MODULE
// FILE: app.cj
package sample.analysis.scope.app

func cons<caret>ume(): Int64 {
    return provide()
}

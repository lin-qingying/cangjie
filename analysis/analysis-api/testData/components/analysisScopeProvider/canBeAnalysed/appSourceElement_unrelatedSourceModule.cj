// MODULE: app
// MODULE_KIND: Source
// MAIN_MODULE
// FILE: app.cj
package sample.analysis.scope.app

func cons<caret>ume(): Int64 {
    return 2
}

// MODULE: unrelated
// MODULE_KIND: Source
// USE_SITE_MODULE
// FILE: unrelated.cj
package sample.analysis.scope.unrelated

func unrelated(): Int64 {
    return 3
}

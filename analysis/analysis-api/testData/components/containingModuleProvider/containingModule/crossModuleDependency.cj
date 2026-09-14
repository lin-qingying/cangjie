// MODULE: dependency
// MODULE_KIND: Source
// FILE: dependency.cj
package sample.resolver.modules.dependency

public func provide(): Int64 {
    return 1
}

// MODULE: app(dependency)
// MODULE_KIND: Source
// MAIN_MODULE
// FILE: app.cj
package sample.resolver.modules.app

import sample.resolver.modules.dependency.*

func useProvide(): Int64 {
    return provide()
}

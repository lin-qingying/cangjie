// MODULE: lib
// MODULE_KIND: LibraryBinaryDecompiled
// FILE: lib.cj
package sample.lib

public type RemoteAlias = Int64

public func echo(value: RemoteAlias): RemoteAlias {
    return value
}

// MODULE: main(lib)
// MAIN_MODULE
// MAIN_FILE_NAME: main.cj
// FILE: main.cj
package sample.main

import sample.lib.RemoteAlias

func useAlias(value: <caret>RemoteAlias): RemoteAlias {
    return value
}

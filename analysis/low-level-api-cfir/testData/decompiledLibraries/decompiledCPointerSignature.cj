// MODULE: lib
// MODULE_KIND: LibraryBinaryDecompiled
// FILE: lib.cj
package sample.lib

import std.core.CPointer

@C
public struct ProcessInfo {}

public func echoProcessInfo(value: CPointer<ProcessInfo>): CPointer<ProcessInfo> {
    return value
}

// MODULE: main(lib)
// MAIN_MODULE
// MAIN_FILE_NAME: main.cj
// FILE: main.cj
package sample.main

func use(): Unit {}

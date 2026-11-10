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


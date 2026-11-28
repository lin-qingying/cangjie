// MODULE: removedArtifact
// FILE: removed.cj
package g6.fixture

public class RemovedDeclaration {
    public init(value: Int64) {}
}

// MODULE: lib
// MODULE_KIND: LibraryBinaryDecompiled
// FILE: current.cj
package g6.fixture

public class CurrentMarker {
    public init() {}
}

// MODULE: main(lib)
// MODULE_KIND: LibrarySource
// MAIN_MODULE
// MAIN_FILE_NAME: main.cj
// FILE: main.cj
package g6.use

public func useLibrary(): Unit {}

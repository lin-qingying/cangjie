// MODULE: sampleLibrary
// MODULE_KIND: LibrarySource
// MAIN_MODULE
// MAIN_FILE_NAME: librarySource.cj
// EXPECTED_PRIMARY_MODULE_SHAPE: LibrarySourceModule
// EXPECTED_BINARY_ARTIFACT_MODULE_SHAPE: LibraryBinaryModule
// EXPECTED_AUXILIARY_MODULE_SHAPE: BuiltinsModule
// EXPECTED_DIRECT_REGULAR_DEPENDENCY: <test-builtins>
// EXPECTED_IS_RESOLVABLE: true
// FILE: librarySource.cj
package sample.library

class LibraryType {
}

func exported(): LibraryType {
    return LibraryType()
}

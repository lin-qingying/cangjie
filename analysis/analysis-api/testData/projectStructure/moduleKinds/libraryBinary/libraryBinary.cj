// MODULE: sampleBinaryLibrary
// MODULE_KIND: LibraryBinary
// FALLBACK_DEPENDENCIES
// MAIN_MODULE
// MAIN_FILE_NAME: libraryBinary.cj
// EXPECTED_PRIMARY_MODULE_SHAPE: LibraryBinaryModule
// EXPECTED_AUXILIARY_MODULE_SHAPE: LibraryFallbackDependenciesModule
// EXPECTED_DIRECT_REGULAR_DEPENDENCY: sampleBinaryLibrary.fallback
// EXPECTED_IS_RESOLVABLE: false
// FILE: libraryBinary.cj
package sample.library

class LibraryType {
}

func exported(): LibraryType {
    return LibraryType()
}

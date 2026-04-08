// MODULE: regularCarrier
// FILE: regularCarrier.cj
package sample.regular

class RegularCarrier {
}

// MODULE: friendCarrier
// FILE: friendCarrier.cj
package sample.friend

class FriendCarrier {
}

// MODULE: fallbackOwner(regularCarrier)
// MODULE_KIND: LibraryFallbackDependencies
// MAIN_MODULE
// MAIN_FILE_NAME: libraryFallbackDependencies.cj
// EXPECTED_PRIMARY_MODULE_SHAPE: LibraryFallbackDependenciesModule
// EXPECTED_AUXILIARY_MODULE_SHAPE: BuiltinsModule
// EXPECTED_DIRECT_REGULAR_DEPENDENCY: <test-builtins>
// EXPECTED_DIRECT_REGULAR_DEPENDENCY: regularCarrier
// EXPECTED_IS_RESOLVABLE: false
// FILE: libraryFallbackDependencies.cj
package sample.fallback

class FallbackCarrier {
}

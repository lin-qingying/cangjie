// MODULE: helper(peer)
// FILE: helper.cj
package sample.outside

class Helper {
}

func helperFactory(): Helper {
    return Helper()
}

// MODULE: peer(helper)
// FILE: peer.cj
package sample.outside

class Peer {
}

func peerFactory(): Peer {
    return Peer()
}

// MODULE: outside(helper)
// MODULE_KIND: NotUnderContentRoot
// MAIN_MODULE
// MAIN_FILE_NAME: outside.outsideRoot.cj
// EXPECTED_PRIMARY_MODULE_SHAPE: NotUnderContentRootModule
// EXPECTED_BINARY_ARTIFACT_MODULE_SHAPE: LibraryBinaryModule
// EXPECTED_AUXILIARY_MODULE_SHAPE: BuiltinsModule
// EXPECTED_AUXILIARY_MODULE_SHAPE: LibraryFallbackDependenciesModule
// EXPECTED_DIRECT_REGULAR_DEPENDENCY: <test-builtins>
// EXPECTED_DIRECT_REGULAR_DEPENDENCY: helper
// EXPECTED_DIRECT_REGULAR_DEPENDENCY: outside.fallback
// EXPECTED_ORIGINAL_MODULE: helper
// EXPECTED_IS_RESOLVABLE: true
// FILE: outside.outsideRoot.cj
package sample.outside

func useOutsideRoot(): Helper {
    return helperFactory()
}

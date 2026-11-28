// LANGUAGE_VERSION: 1.1.3

package returned_modifiers.functions

foreign {
    func inheritedNative(): Unit
    unsafe func conflictingNative(): Unit
}

func ordinaryFunction(): Unit {}
unsafe func unsafeFunction(): Unit {}

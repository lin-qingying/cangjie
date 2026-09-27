// FILE: ambiguityByConversion.cj
// TARGET_CALL: choose(fail(error))
package sample.resolver.candidates

func fail(error: Error): Nothing {
    throw error
}

func choose(value: Int64): Unit {}
func choose(value: String): Unit {}

func callWithBottomType(error: Error): Unit {
    choose(fail(error))
}

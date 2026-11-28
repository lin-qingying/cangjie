package test.hosts

func useCatch(): Unit {
    try {
        throw Exception()
    } catch (failure: Exception) {
        let captured = <caret_catchUse>failure
    }
    try {
        throw Exception()
    } catch (_: Exception) {}
}

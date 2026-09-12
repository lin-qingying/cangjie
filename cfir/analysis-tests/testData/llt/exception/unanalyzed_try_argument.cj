func noArgs(): Unit {}

func use(): Unit {
    let broken = <!GENERIC_ARGUMENT_NO_MATCH!>noArgs<Int64>()<!>
    broken(try {} catch (_: Nothing) {})
    broken(try (resource = 1) {})
    broken(try {} catch (_: Exception | Error) {})
}



internal import std.fs.*
internal import std.io.*

struct Exception {
    let foo: Int64 = 314
}

main() {
    try (input = File("throwable_00.cj", Read)) {
        let data = readToEnd(input)
        println("try")
    } catch (e: FSException) {
        println("catch")
    } finally {
        println("finally")
    }
}

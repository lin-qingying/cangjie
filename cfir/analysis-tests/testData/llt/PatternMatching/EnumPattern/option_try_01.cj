

internal import std.fs.*
internal import std.io.*

enum Option<T> {
    | Some(T)
    | None
    | WhatsThis
}

main() {
    try (input = File("option_try_00.cj", Read)) {
        let data = readToEnd(input)
        println("try")
    } catch (e: FSException) {
        println("catch")
    } finally {
        println("finally")
    }
}

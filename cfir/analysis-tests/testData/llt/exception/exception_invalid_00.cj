


class A <: Exception {}

main() {
    try {
        throw A()
    } catch <!TYPE_INCOMPATIBLE!>(e: A | Error)<!> {
        println(e)
    }
}

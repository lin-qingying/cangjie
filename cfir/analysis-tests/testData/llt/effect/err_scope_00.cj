import stdx.effect.Command

class Eff <: Command<Int64> {}

main() {
    try {
    } handle (e: Eff) {
        let <!REDECLARATION!>e<!> = 0
        println(e)
    }
}

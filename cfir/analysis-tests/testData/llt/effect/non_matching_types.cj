import stdx.effect.Command

class Eff <: Command<Unit> {}

main() {
    let x = try {
      34
    } <!MISMATCHING_HANDLE_BLOCK!>handle (_: Eff)<!> {
      "Hello"
    }
    0
}

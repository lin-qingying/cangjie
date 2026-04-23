import stdx.effect.Command

class Eff <: Command<Unit> {}

main():Int64 {
    try {

    } handle (_: Eff) {
        resume throwing Exception()
    } 
    return 0
}

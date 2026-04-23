import stdx.effect.Command

class Eff <: Command<Unit> {}

main():Int64 {
    try {

    } handle (_: Eff) {
        resume
    } 
    return 0
}

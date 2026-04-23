import stdx.effect.Command

class Eff <: Command<Int64> {}

main():Int64 {
    try {

    } handle (_: Eff) {
        resume with 43
    } 
    return 0
}

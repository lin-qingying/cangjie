import stdx.effect.Command


class Eff <: Command<Int64> {}

public func assertPerformed<T>(fn: ()->Int64, msg!: String = ""): Int64 where T <: Command<Int64> {
    try{
        return fn()
    } handle (e: T){
        resume with 11
    }
}

main():Int64 {
    let a = assertPerformed<Eff>({ => perform Eff() })
    if (a != 11) {
        return 1
    }
    return 0
}

import stdx.effect.Command

class Effect <: Command<String> {}

main():Int64 {
    try {
        perform Effect();
    } handle (e: Effect) {
        print("in handle block!")
        resume with "result"
    } finally {
        print("in finally block!")
    }
    return 0
}

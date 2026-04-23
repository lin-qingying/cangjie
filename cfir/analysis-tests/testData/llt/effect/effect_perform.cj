import stdx.effect.Command
class Eff <: Command<String> {}

main():Int64 {
    try {
            perform Eff();
            var a = 1
    } handle (e: Eff) {
            print("in handle block!")
            resume with "Result"
    } finally {
        print("in finally block!")
    }
    return 0
}

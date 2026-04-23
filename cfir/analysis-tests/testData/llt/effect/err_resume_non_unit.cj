import stdx.effect.Command

main() {
    try {}
    handle (e: Command<Int64>) {
        <!RESUME_NO_WITH!>resume<!>
    }
    0
}

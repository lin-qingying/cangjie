import stdx.effect.Command

main() {
    try {}
    handle (e: Command<Int64>) {
        <!TYPE_MISMATCH!>resume with "Hello!"<!>
    }
    0
}

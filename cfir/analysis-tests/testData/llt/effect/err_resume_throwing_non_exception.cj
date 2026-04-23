import stdx.effect.Command

main() {
    try {}
    handle (e: Command<Int64>) {
        <!RESUME_THROWING_MISMATCH_TYPE!>resume throwing "Hello!"<!>
    }
    0
}

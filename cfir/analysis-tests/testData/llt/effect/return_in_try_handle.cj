import stdx.effect.Command

main():Int64 {
    try {

    } handle (_: Command<Unit>) {
        <!RETURN_IN_TRY_HANDLE_BLOCK!>return 1<!>
    }
}

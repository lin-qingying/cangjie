


main() {
    var flag = false
    <!USE_FUNC_CAPTURE_VAR_ALONE!>spawn {
        =>
        var a = 1
        if (a + 1 == 2) {
            flag = true
        }
    }<!>
    return
}

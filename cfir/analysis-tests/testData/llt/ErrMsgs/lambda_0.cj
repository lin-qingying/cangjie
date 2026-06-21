


main() {
    let err = 1 <!INVALID_BINARY_OPERATOR!>+<!> false

    let lam = {
        =>
        err;
        err
    }
    let lam2: () -> Bool = {
        =>
        err;
        err
    }
    {=> err}()

    0
}

// Errors:

// No errors:

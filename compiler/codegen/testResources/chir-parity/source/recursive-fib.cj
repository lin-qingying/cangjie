func fib(n: Int64): Int64 {
    if (n <= 1) {
        return n
    }
    return fib(n - 1) + fib(n - 2)
}

main(): Int64 {
    return fib(6)
}

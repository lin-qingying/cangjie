import stdx.effect.Command

// Was ICE, should report error
func foo(a: Array<Int64>) : Int64 {1}
main() : Int64 {
    foo([try {
        1
    } handle (e: Command<String>) {
        <!UNRESOLVED_REFERENCE!>f<!> // undefined
        resume with "Result"
    }])
}

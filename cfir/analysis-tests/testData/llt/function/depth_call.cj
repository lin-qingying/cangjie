


enum bbb {
    center | Month | Day | Hour
}

func aaa(a: Int32) {
    0
}

func aaa(ctx!: Int64, callback!: Option<(Int64, Int64) -> Unit> = Option<(Int64, Int64) -> Unit>.None) {}

func ccc(a: Int16): Int16 {
    1
}

func ccc(a: Int32): Int32 {
    1
}

func ccc(a!: Int32, b!: Int32): Int32 {
    1
}

func Test() {
    aaa(
        ctx: 10,
        callback: Option<(Int64, Int64) -> Unit>.Some(
            {
                start: Int64, end: Int64 =>
                let a = Some<bbb>(bbb.<!NOT_MEMBER_OF!>cente<!>)
                return
            }
        )
    )

    aaa(<!TYPE_MISMATCH!>ccc(Int16(1))<!>)
    aaa(ccc(<!CANNOT_CONVERT_LITERAL!>r<!>'a'))
    aaa(ccc(a: r'a', <!UNORDERED_ARGUMENTS!>1<!>))
    aaa(ccc(a: r'a', <!MULTIPLE_NAMED_ARGUMENT!>a<!>: 1))
    aaa(ccc(a: <!CANNOT_CONVERT_LITERAL!>r<!>'a', b: 1))
    return 0
}

main() {
    Test()
    return 10
}

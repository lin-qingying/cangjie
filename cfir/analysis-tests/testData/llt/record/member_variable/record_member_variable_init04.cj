



struct A {
    public let b: Int64 = 1
    public static var a: () -> Int64 = {=> <!ILLEGAL_USAGE_OF_MEMBER, STATIC_LAMBDA_CANNOT_ACCESS_NON_STATIC!>b<!>}
}


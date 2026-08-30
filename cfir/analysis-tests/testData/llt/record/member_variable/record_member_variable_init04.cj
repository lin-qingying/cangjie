



struct A {
    public let b: Int64 = 1
    public static var a: () -> Int64 = {=> <!STATIC_LAMBDA_CANNOT_ACCESS_NON_STATIC!>b<!>}
}





class A {
    public let x: Int64 = 1
}

extend A {
    public func foo(): Int64 {
        return this.<!NOT_MEMBER_OF!>y<!>
    }
}

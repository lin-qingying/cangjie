


<!VALUE_TYPE_RECURSIVE!>s<!>truct A {
    public let <!CLASS_UNINITIALIZED_FIELD!>x<!>: A
}

<!VALUE_TYPE_RECURSIVE!>s<!>truct B {
    public let <!CLASS_UNINITIALIZED_FIELD!>x<!>: C
    public let <!CLASS_UNINITIALIZED_FIELD!>y<!>: Int32
}

struct C {
    public let <!CLASS_UNINITIALIZED_FIELD!>x<!>: D
}

struct D {
    public let <!CLASS_UNINITIALIZED_FIELD!>x<!>: B
}

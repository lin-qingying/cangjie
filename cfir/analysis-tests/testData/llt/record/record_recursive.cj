


<!VALUE_TYPE_RECURSIVE!>s<!>truct A {
    <!CLASS_UNINITIALIZED_FIELD!>p<!>ublic let x: A
}

<!VALUE_TYPE_RECURSIVE!>s<!>truct B {
    <!CLASS_UNINITIALIZED_FIELD!>p<!>ublic let x: C
    <!CLASS_UNINITIALIZED_FIELD!>p<!>ublic let y: Int32
}

struct C {
    <!CLASS_UNINITIALIZED_FIELD!>p<!>ublic let x: D
}

struct D {
    <!CLASS_UNINITIALIZED_FIELD!>p<!>ublic let x: B
}

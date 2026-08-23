



struct Test {
    private Test(var name: String) {}
    <!CLASS_UNINITIALIZED_FIELD, CONFLICTING_OVERLOADS!>init<!>(name: String) {}
}


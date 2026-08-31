

open class C1 {
    private init() {
    }
}

class C2 <: C1 {
    public <!NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS!>init<!>() {
    }
}

main() {
    return 0
}

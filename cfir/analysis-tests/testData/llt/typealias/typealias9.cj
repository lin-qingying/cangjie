


type A<T> = Cl<T>

open class Base {}

class Cl<U> where U <: Base {}

class Sub<X> {
    public var <!CLASS_UNINITIALIZED_FIELD!>a<!>: <!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>A<!><X>
}

main() {
    return 0;
}

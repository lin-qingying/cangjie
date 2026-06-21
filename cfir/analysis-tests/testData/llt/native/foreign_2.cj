

interface I<T> {}

open class A<T> {}

class B<T> <: A<T> & I<T> {}

foreign func assert(input: <!INVALID_CFUNC_PARAMETER_TYPE!>B<Int32><!>): Int32




interface I1 {}

class Foo <: I1 {}

extend Foo <: <!EXTEND_DUPLICATE_INTERFACE!>I1<!> {}

interface I2<T> {}

type i2<T> = I2<T>

class Goo<T> <: I2<Int32> {}

extend<T> Goo<T> <: <!EXTEND_DUPLICATE_INTERFACE!>i2<Int32><!> {}

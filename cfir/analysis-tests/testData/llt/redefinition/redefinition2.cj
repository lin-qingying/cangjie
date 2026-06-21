


interface I1 {}

interface I2 {}

class A {
    func f<T>(a: T) where T <: I1 {}
    func f<T>(a: T) where T <: I2 {} // Error, not overloading
    func <!CONFLICTING_OVERLOADS!>f<!><T>(a: T) {} // Error, not overloading
}

func f<T>(a: T) where T <: I1 {}

func f<T>(a: T) where T <: I2 {} // Error, not overloading

func <!CONFLICTING_OVERLOADS!>f<!><T>(a: T) {} // Error, not overloading

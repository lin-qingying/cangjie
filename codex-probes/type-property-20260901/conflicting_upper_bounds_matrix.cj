
open class Animal {}
open class Vehicle {}
struct Token {}

class Kennel<T> where T <: Animal & Vehicle {}

class C1 {}
class A1<T> where T <: C1 {}
class B1<X> where X <: A1<X> {}
class D1<U, V> where U <: B1<U>, V <: A1<V> {}
class E1<T> where T <: E1<T> {}
class F1<T> where T <: G1<T> {}
class G1<T> where T <: F1<T> {}

interface I1 {}
class D2<T> {}
func bar<T, U>() where T <: D2<U> & D2<I1>, U <: I1 {}

open class Base1 {}
class Balala1 {}
func foo<T>() where T <: Base1 & Balala1 {}

class X1 {}
class Y1 {}
class A2<T, K> where T <: X1, K <: Y1 {}
extend<T> A2<T, T> {}

main() {}

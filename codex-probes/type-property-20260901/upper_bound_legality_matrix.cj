
class C1 {}

class A<T> where T <: C1 {}

class B<X> where X <: A<X> {}

class C<X> where X <: A<C1> {}

class D<T> {}

func f<X>() where X <: D<C1> & D<Int64> {}

main() {}

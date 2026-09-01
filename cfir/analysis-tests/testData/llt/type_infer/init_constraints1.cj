


class C1 {}

class A<T> where T <: C1 {}

class B<<!MULTIPLE_CLASS_UPPER_BOUNDS!>X<!>> where X <: A<X> {}

class D<<!MULTIPLE_CLASS_UPPER_BOUNDS!>U<!>, <!MULTIPLE_CLASS_UPPER_BOUNDS!>V<!>> where U <: B<U>, V <: A<V> {}

class E<T> where T <: E<T> {}

class F<<!MULTIPLE_CLASS_UPPER_BOUNDS!>T<!>> where T <: G<T> {}

class G<<!MULTIPLE_CLASS_UPPER_BOUNDS!>T<!>> where T <: F<T> {}

class I<T, X> where T <: C1 {}

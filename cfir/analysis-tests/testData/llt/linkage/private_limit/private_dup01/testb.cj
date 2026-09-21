// FILE: testa.cj
package testa

private class <!EXPORT_SAME_PRIVATE_DECL!>A<!> {}

public class D<T> {
    private let a: A = A()
}



// FILE: testb.cj
package testa

private class A {}

public class C<T> {
    private let a: A = A()
}



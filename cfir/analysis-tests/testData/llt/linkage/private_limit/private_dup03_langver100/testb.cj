// LANGUAGE_VERSION: 1.0.0

// FILE: testa.cj
package testa

private class A {}

public class D {
    private func f(a: A) {}
}



// FILE: testb.cj
package testa

private class A {}

public class C {
    private func f(a: A) {}
}

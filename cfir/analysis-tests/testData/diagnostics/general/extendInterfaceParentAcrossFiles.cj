// FILE: api.cj
package sample

interface A {}

class B {}

// FILE: extend.cj
package sample

extend B <: A {}

func acceptA(value: A): Unit {
    let _ = value
}

// FILE: main.cj
package sample

main(): Unit {
    acceptA(B())
}

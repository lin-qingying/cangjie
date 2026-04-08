// FILE: api.cj
package sample

interface A {}

class B {}

// FILE: extend.cj
package sample

extend B <: A {}

// FILE: main.cj
package sample

main() {
    let a: A = B()
}

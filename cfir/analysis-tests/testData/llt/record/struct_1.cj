// MODULE: data
// DEPENDENCE: struct_data.cj
// FILE: data_module.cj
// MODULE: main(data)
// FILE: struct_1.cj



package example

import example.data.*

main() {
    let d = Data()
    d.foo()
    d.a_ + d.a
}

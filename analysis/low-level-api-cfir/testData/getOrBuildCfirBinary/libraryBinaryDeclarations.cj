// MODULE: binaryLibrary
// MODULE_KIND: LibraryBinary
// MAIN_MODULE
// MAIN_FILE_NAME: libraryBinaryDeclarations.cj
// FILE: libraryBinaryDeclarations.cj
package lowlevel.getorbuild.binary

class Box<T> {
    let value: T
    let count: <expr_3>Int64</expr_3> = 0
    init(value: T) {
        this.value = value
    }
}

func wrap(value: Int64): Box<Int64> {
    return Box(<expr>value</expr>)
}

func unwrap(box: Box<Int64>): Int64 {
    return <expr_1>box.value</expr_1>
}

func main(): Int64 {
    let box = <expr_2>wrap(41)</expr_2>
    return unwrap(box)
}

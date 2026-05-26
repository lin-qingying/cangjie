// FILE: basic.cj
package lowlevel.getorbuild

class Box<T> {
    let value: T
    init(value: T) {
        this.value = value
    }
}

func wrap(value: Int64): Box<Int64> {
    return Box(value)
}

func unwrap(box: Box<Int64>): Int64 {
    return <expr>box.value</expr>
}

func main(): Int64 {
    let box = <expr_1>wrap(41)</expr_1>
    return <expr_2>unwrap(box)</expr_2>
}

// FILE: api.cj
package api
public interface I {
    func f(): Int64
}
extend Int64 <: I {
    public func f(): Int64 { 111 }
}
public func scalar(value: I): Int64 { value.f() }
public func array(value: Array<I>): Int64 { value[0].f() }
public func nested(value: Array<Array<I>>): Int64 { value[0][0].f() }
public func nestedNumbers(value: Array<Array<Int64>>): Int64 { value[0][0] }

// FILE: main.cj
import api.*
main() {
    scalar(1)
    array([1])
    array([1i64])
    array([1, 2])
    let number: Int64 = 1
    array([number])
    nested([[1]])
    nested([1])
    nestedNumbers([1])
    let handler: (Array<I>) -> Int64 = array
    handler([1])
    let value: I = 1
    let values: Array<I> = [1]
    array(values)
    let inferred = [1]
    array(<!ARGUMENT_TYPE_MISMATCH!>inferred<!>)
    return 0
}

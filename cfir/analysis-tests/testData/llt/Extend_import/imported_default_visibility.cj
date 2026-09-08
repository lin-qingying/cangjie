// FILE: a.cj
package a
public interface IA {
    prop code: Int64 { get() { 1 } }
}
extend Int64 <: IA {}

// FILE: b.cj
package b
public interface IB {
    prop code: Int64 { get() { 2 } }
}
extend Int64 <: IB {}
public class Unrelated {}

// FILE: main.cj
import a.IA
import b.Unrelated
main() {
    let x: Int64 = 1
    println(x.code)
    Unrelated()
    return 0
}

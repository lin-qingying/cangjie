// FILE: base.cj
package visibilityprivate
class Counter {
    private let secret: Int64 = 42
    private func secretFunc(): Int64 { return 7 }
}
// FILE: use.cj
package visibilityprivate.other
import visibilityprivate.*
func readSecret(counter: Counter): Int64 { return counter.secret }
func callSecret(counter: Counter): Int64 { return counter.secretFunc() }

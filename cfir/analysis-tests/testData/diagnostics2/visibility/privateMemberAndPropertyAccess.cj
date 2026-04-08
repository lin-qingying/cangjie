// RUN_PIPELINE_TILL: FRONTEND
// FILE: base.cj
package visibilityprivate

class Counter {
    private let secret: Int64 = 42

    private func secretFunc(): Int64 {
        return 7
    }

    public func visible(): Int64 {
        return secret + this.secretFunc()
    }
}

// FILE: use.cj
package visibilityprivate.other
import visibilityprivate.*

func readSecret(counter: Counter): Int64 {
    return counter.<!INVISIBLE_MEMBER!>secret<!>
}

func callSecret(counter: Counter): Int64 {
    return counter.<!INVISIBLE_MEMBER!>secretFunc<!>()
}

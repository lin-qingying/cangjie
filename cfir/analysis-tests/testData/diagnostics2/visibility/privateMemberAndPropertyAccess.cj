// RUN_PIPELINE_TILL: FRONTEND
// FILE: base.cj
package visibilityprivate

public class Counter {
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
    return <!INVALID_ACCESS_CONTROL!>counter<!>.secret
}

func callSecret(counter: Counter): Int64 {
    return counter.<!NOT_MEMBER_OF!>secretFunc<!>()
}

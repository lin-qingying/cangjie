

package dep

import ohos.labels.*

@!APILevel[9]
public func f9(): Unit {
    f11() // error
    f0() // ok
}

@!APILevel[11]
public func f11(): Unit {
    f9() // ok
    f0() // ok
}

// 没写，相当于 @!APILevel[0]
public func f0(): Unit {
    f9() // error
    f11() // error
}

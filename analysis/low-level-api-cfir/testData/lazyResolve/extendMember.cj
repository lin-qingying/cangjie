// MODULE: main
// FILE: common.cj
package sample.lowlevel.lazy.extend.common

public open class Host<T> {
    public var value: T

    public init(value: T) {
        this.value = value
    }

    public func size(): Int64 {
        return 1
    }
}

// FILE: main.cj
package sample.lowlevel.lazy.extend

import sample.lowlevel.lazy.extend.common.Host

interface Marker {}

extend <T> Host<T> <: Marker {
    // T 来自 extend 的类型参数，this 指向被扩展类型 Host<T>；两者都只能由 extend 作用域提供。
    func <caret>describe(): String {
        let current: T = this.value
        return this.size().toString() + current.toString()
    }
}
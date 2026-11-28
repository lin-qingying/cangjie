// 关键字（一）：包 / 模块、类型声明、控制流、可见性与静态成员关键字，
// 全部置于合法语法位置（cjc 1.0.5 可编译验证）。
// 官方依据：Cangjie 文档《关键字》；cjc 1.0.5 / 1.1.3 零诊断。
// 说明：perform / resume / handle / throwing（代数效果）与 macro / common / specific
// 需要特定编译形态或平台，见 parsing/expressions/effectAlgebra.cj 与 declarations/macroCalls.cj。
package keywordforms

import std.math.*
import std.sync.*

public open class BaseForm {
    public var count: Int64 = 0

    public init() {}

    public open func compute(v: Int64): Int64 {
        return v
    }
}

public abstract class ShapeForm {
    public init() {}

    public func area(): Float64
}

public struct PointForm {
    public var x: Int64 = 0

    public init() {}

    public mut func reset(): Unit {
        this.x = 0
    }
}

public interface DisplayForm {
    func show(): String
}

public enum ColorForm {
    | Red
    | Green
}

extend Int64 {
    public func doubled(): Int64 {
        return this * 2
    }
}

type Handler = (Int64) -> Int64

public class Statics {
    public static func s(): Int64 {
        return 1
    }

    public static prop limit: Int64 {
        get() {
            return 2
        }
    }
}

public class DerivedForm <: BaseForm {
    public init() {
        super()
    }

    public override func compute(v: Int64): Int64 {
        return super.compute(v) + this.count
    }
}

public func controlFlow(xs: Array<Int64>): Int64 {
    var total = 0
    for (x in xs) {
        if (x % 2 == 0) {
            continue
        } else {
            total += x
        }
        while (total > 100) {
            break
        }
        do {
            total++
        } while (false)
    }
    match (total) {
        case 0 => total = 1
        case _ => ()
    }
    let flag: Bool = true
    let none: ?Int64 = None
    try {
        throw Exception("e")
    } catch (e: Exception) {
        let _ = e
    } finally {
        let _ = 0
    }
    let maybe: ?Int64 = 5
    if (let Some(bound) <- maybe) {
        total += bound
    }
    let lock = ReentrantMutex()
    synchronized(lock) {
        total = total
    }
    return total
}

public func unsafeForm(): Int64 {
    return unsafe { 1 }
}

public func spawnForm(): Int64 {
    let v: Int64 = 1
    let f = spawn {
        => v
    }
    let _ = f
    return v
}

public func whereForm<T>(a: T, b: T): T where T <: Comparable<T> {
    return if (a < b) {
        a
    } else {
        b
    }
}

public func castForm(x: Any): Int64 {
    if (x is Int64) {
        let n = (x as Int64).getOrThrow()
        return n
    }
    return 0
}

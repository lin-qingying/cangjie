// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class Cls<X> {
    public var a: X
    public init(x: X) {
        a = x
    }
    public func test<T>(v: T) {
        return v
    }
}

struct Rcd<X> {
    public var a: X
    public init(x: X) {
        a = x
    }
    public func test<T>(v: T) {
        return v
    }
}

enum EN<X> {
    E | E1(X)

    public func test<T>(v: T) where T <: Countable<T> {
        return match (this) {
            case E1(x) => v.next(2)
            case E => v.next(1)
        }
    }
}

extend<X> Rcd<X> {
    public func foo<T>(v: T) {
        return v
    }
}

main(): Int64 {
    let cls = Cls<Int16>(1)
    if (cls.test<Int64>(1111) != 1111) {
        return 1
    }

    let rd = Rcd<Int32>(2)
    if (rd.test<Int64>(222) != 222) {
        return 2
    }

    if (rd.foo(123) != 123) {
        return 3
    }

    if (E<Int8>.test<Int64>(1) != 2) {
        return 4
    }

    let e = E1(2)
    if (e.test<Int64>(1) != 3) {
        return 5
    }
    return 0
}

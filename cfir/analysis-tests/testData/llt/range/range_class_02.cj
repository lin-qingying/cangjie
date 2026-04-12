// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

class A {
    var a: Int64
    init(x: Int64) {
        a = x
    }
}

extend A <: Countable<A> {
    public func next(r: Int64): A {
        return A(this.a + r)
    }
    public func position(): Int64 {
        return this.a
    }
}

extend A <: Comparable<A> {
    public func compare(rhs: A): Ordering {
        match {
            case this.a < rhs.a => Ordering.LT
            case this.a > rhs.a => Ordering.GT
            case _ => Ordering.EQ
        }
    }
    public operator func <(rhs: A): Bool {
        this.a < rhs.a
    }
    public operator func <=(rhs: A): Bool {
        this.a <= rhs.a
    }
    public operator func >(rhs: A): Bool {
        this.a > rhs.a
    }
    public operator func >=(rhs: A): Bool {
        this.a >= rhs.a
    }
    public operator func ==(rhs: A): Bool {
        this.a == rhs.a
    }
    public operator func !=(rhs: A): Bool {
        this.a != rhs.a
    }
}

interface I {}

extend<T> Range<T> <: I {}

main(): Int64 {
    var v0: Range<A> = A(1)..A(10)
    let v1: I = A(1)..A(10)
    let v2: Any = A(1)..A(10)
    var size = 0
    for (i in v0) {
        size++
    }
    if (size != 9) {
        return 1
    }
    if (!(v1 is Range<A>)) {
        return 1
    }
    return 0
}

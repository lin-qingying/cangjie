// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

class A<T> where T <: Countable<T> & Comparable<T> & Equatable<T> {
    var a: T
    init(x: T) {
        a = x
    }
}

extend<T> A<T> <: Countable<A<T>> {
    public func next(r: Int64): A<T> {
        return A(this.a.next(r))
    }
    public func position(): Int64 {
        return this.a.position()
    }
}

extend<T> A<T> <: Comparable<A<T>> {
    public func compare(rhs: A<T>): Ordering {
        match {
            case this.a < rhs.a => Ordering.LT
            case this.a > rhs.a => Ordering.GT
            case _ => Ordering.EQ
        }
    }
    public operator func <(rhs: A<T>): Bool {
        this.a < rhs.a
    }
    public operator func <=(rhs: A<T>): Bool {
        this.a <= rhs.a
    }
    public operator func >(rhs: A<T>): Bool {
        this.a > rhs.a
    }
    public operator func >=(rhs: A<T>): Bool {
        this.a >= rhs.a
    }
    public operator func ==(rhs: A<T>): Bool {
        this.a == rhs.a
    }
    public operator func !=(rhs: A<T>): Bool {
        this.a != rhs.a
    }
}

interface I<T> {}

extend<T> Range<T> <: I<T> {}

main(): Int64 {
    var v0: Range<A<Int32>> = A(1)..A(10)
    let v1: I<A<Int32>> = A(1)..A(10)
    var size = 0
    for (i in v0) {
        size++
    }
    if (size != 9) {
        return 1
    }
    if (!(v1 is Range<A<Int32>>)) {
        return 1
    }
    return 0
}

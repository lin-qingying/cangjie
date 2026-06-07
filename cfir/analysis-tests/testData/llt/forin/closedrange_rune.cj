// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main() {
    for (i in r'a'..=r'c') {
        println(i)
    }
    core()
}

class M <: Comparable<M> & Countable<M> & ToString {
    M(let a: Int64) {}

    public func toString() {
        a.toString()
    }

    public func compare(that: M): Ordering {
        return a.compare(that.a)
    }
    public func next(right: Int64): M {
        M(a + right)
    }
    public func position() {
        a
    }
}

func core() {
    for (i in M(3)..=M(7)) {
        println(i)
    }
}

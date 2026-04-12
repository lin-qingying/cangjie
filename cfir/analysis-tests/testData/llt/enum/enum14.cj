// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

enum TimeUnit1 {
    Year(Int32) | Day(TimeUnit1) | Out(TimeUnit1)
}

enum TimeUnit1_2 {
    Year(Int64) | Day(TimeUnit1_2)
}

class A {}

class B {}

func f(a: A) {
    a
}

func f(a: B) {
    a
}

func g(a: Int64): A {
    A()
}

func g(a: Int32): B {
    B()
}

func h(a: A) {}

func foo1() {
    var m: Int64 = 1
    var b = Day(Year(m))
    var n: Int32 = 2
    var c = Day(Year(n))
    var d = Out(Day(Year(3)))
    var a0 = match (b) {
        case Day(Year(1)) => 0
        case _ => 2
    }
    var a1 = match (c) {
        case Day(Year(2)) => 0
        case _ => 2
    }
    var a2 = match (d) {
        case Out(Day(Year(3))) => 0
        case _ => 2
    }
    if (a0 + a1 + a2 == 0) {
        return 0
    }
    return 1
}

main(): Int64 {
    h(f(g(1)))
    if (foo1() != 0) {
        return 1
    }
    return 0
}

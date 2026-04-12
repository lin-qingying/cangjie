// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class A {
    public A(a: Int64, var b!: Int64 = a + 1) {}
}

func foo0(a: Int64, b!: Int64 = a + 1, c!: Int64 = a * b) {
    c
}

var x: Int64 = 3
var y: Int64 = 2

func cal0(a: Int64, b: UInt64) {
    a ** b
}

func cal1(a: Int64, b: UInt64): Int64 {
    a * Int64(b)
}

func foo1(a!: (Int64, UInt64) -> Int64 = cal0, b!: Int64 = x + y, c!: Int64 = {=> a(y, UInt64(b)) + b}()) {
    b + c
}

func run(): Int64 {
    var x = foo0(2);
    var a = A(3)
    var y = foo1();
    var y2 = foo1(a: cal1, b: 2)
    print("y: ${y2}\n")
    if (x == 6 && a.b == 4 && y == 42 && y2 == 8) {
        return 0
    }
    return 1
}

main(): Int64 {
    return run()
}

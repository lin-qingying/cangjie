// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

func foo(b: Int64): Int64 {
    0
}

func foo<T>(b: T): Int64 {
    1
}

open class A<T> {
    public func get(a: T): Int64 {
        2
    }
    public func get(a: Int32): Int64 {
        3
    }
}

main() {
    var a: Int64 = 1
    var b: Int64 = 2
    var c = foo(a)
    print("c: ${c}\n")
    var d = A<Bool>()
    if (d.get(1) != 3 || d.get(true) != 2) {
        return 1
    }
    return c
}

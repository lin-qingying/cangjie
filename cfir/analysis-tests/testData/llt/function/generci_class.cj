// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

main(): Int64 {
    return run()
}

func run(): Int64 {
    var res1 = Base().Foo(a: 2)
    if (res1 == 4) {
        return 0
    }
    return 1
}

open class Base {
    public var x: Int64 = 2
    public func Foo(a!: Int64 = x): Int64 {
        return a * 2
    }
}

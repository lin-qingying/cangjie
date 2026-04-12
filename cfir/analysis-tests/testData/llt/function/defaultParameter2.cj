// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

func Foo(a!: Int32 = 8, b!: Int32 = a) {
    return a + b
}

func run(): Int64 {
    var foo: Int32 = Foo()
    if (foo == 16) {
        return 0
    }
    return 1
}

main(): Int64 {
    return run()
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f

main(): Int64 {
    return run()
}

var x: Int64 = 2
let y: String = "yz"

func run(): Int64 {
    var res = Foo(1, "x")
    if (res == "3xyz") {
        return 0
    }
    return 1
}

func Foo(a: Int64, b: String, A!: Int64 = x, B!: String = Foo(1, "x")): String {
    return (a + A).toString() + b + B
}

// ASSERT: regex-not .

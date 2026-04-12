// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

open class A {}

class B <: A {}

func foo<X>(a: X, b: X): Int64 {
    return 1
}

main(): Int64 {
    if (foo<Int64>(1, 2) == 1) {
        return 0
    }
    return 1
}

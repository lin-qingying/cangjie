// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

var num = 3

func g<A>(a: A): A {
    var b: A
    if (num > 0) {
        num--
        b = g<A>(a)
    } else {
        return a
    }
    return b
}

main(): Int64 {
    var res = g<Int32>(123)
    if (res != 123) {
        return 1
    }
    return 0
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

func Foo<T, U>(a: T, b: U): T {
    return a
}

func Bar<T>(a: T) {
    var b: Int32 = 1
    var c = Foo<T, Int32>(a, b)
    return c
}

main(): Int64 {
    var a: Int32 = 1
    var b = Bar<Int32>(a)
    if (b == 1) {
        return 0
    } else {
        return 1
    }
}

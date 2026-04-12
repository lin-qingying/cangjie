// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

func foo<T, U>(a: T, b: U): T {
    func bar<P>(a: P) {
        return a
    }
    return bar<T>(a)
}

main(): Int64 {
    var a: Bool = false
    var b: Int32 = 1
    var res = foo<Bool, Int32>(a, b)

    if (res != false) {
        return 1
    }
    return 0
}

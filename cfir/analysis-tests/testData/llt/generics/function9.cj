// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

func foo<T>(a: T): T {
    return a
}

main() {
    var f = foo<Int32>
    var a = f(1)
    if (a == 1) {
        return 0
    } else {
        return 1
    }
}

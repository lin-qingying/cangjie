// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I<T> {
    func foo(a: T): T {
        return a
    }
}

class B <: I<Int32> {}

main() {
    var obj = B()
    if (obj.foo(1) == 1) {
        return 0
    }
    return 1
}

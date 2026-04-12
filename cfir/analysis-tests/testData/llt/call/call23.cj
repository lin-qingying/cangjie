// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

open class I<T> {
    public func test(a: T) {
        return 1
    }
    public func test(a: Int32) {
        return 2
    }
}

main(): Int64 {
    var res1 = I<Int64>().test((1))
    if (res1 != 1) {
        return 1
    }
    return 0
}

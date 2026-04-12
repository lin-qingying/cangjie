// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

open class C1<T> {
    public func c1(a: Int32) {
        return 0
    }
    public func c2(a: T) {
        return 0
    }
}

class C2 <: C1<Int32> {}

class C3 <: C1<Int64> {}

main() {
    var a = C1<Int32>()
    var b = C1<Int64>()
    return 0
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %noColor %f -o %n.%suffix 2>&1 | compare %f

class Cls<X> {
    public func test(b: X) {
        return b
    }
    public func test<T>(v: T) {
        return v
    }
    public func <!CONFLICTING_OVERLOADS!>test<!><T>(v: X) {
    }
}

main(): Int64 {
    let cls = Cls<Int64>()
    return 0
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

extend Int64 {
    public func add(num: Int64): Int64 {
        return this + num
    }
    public func double(): Int64 {
        return add(this)
    }
}

main() {
    let x: Int64 = 1;
    return x.add(1) - x.double()
}

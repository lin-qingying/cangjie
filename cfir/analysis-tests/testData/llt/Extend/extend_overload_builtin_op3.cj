// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface Eq<T> {
    operator func ==(right: T): Bool
}

extend Int64 <: Eq<Int64> {}

struct Box<T> where T <: Eq<T> {
    public let item: T
    public init(item: T) {
        this.item = item
    }
    public operator func ==(right: Box<T>) {
        return item == right.item
    }
}

main(): Int64 {
    let a = Box<Int64>(1)
    let b = Box<Int64>(1)
    if (a == b) {
        return 0
    }
    return 1
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class Box<T> {
    public let item: T
    public init(item: T) {
        this.item = item
    }
}

extend<T> Box<T> {
    public func getItem(): T {
        return item
    }
}

main() {
    let x = Box<Int64>(0);
    return x.getItem()
}

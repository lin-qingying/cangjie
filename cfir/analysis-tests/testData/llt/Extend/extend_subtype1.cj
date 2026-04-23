// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I {}

extend Int32 <: I {
    public func add() {
        0
    }
}

func test(a: I) {}

main() {
    let a: Int32 = 0
    test(a)
    return a.add()
}

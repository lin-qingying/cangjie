// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class C<M> {
    init(x: M) {}
}

func test2<M>(x: C<M>): C<M> {
    x
}

interface I2<M> {
    func f2(x: M): Int64
}

class C2<M> <: I2<M> {
    public func f2(x: M): Int64 {
        return 1
    }
}

func f2() {
    test2<I2<Int64>>
}

main() {
    var b = f2()(C(C2<Int64>()))
    return 0
}

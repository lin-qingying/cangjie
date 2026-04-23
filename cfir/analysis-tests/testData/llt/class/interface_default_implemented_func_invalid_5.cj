// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    let x: Int32 = 1
    var a = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C<!>(x)
    return 0
}

interface B<T> {
    func foo(x: Int32): Int64 {
        return 1
    }
}

class C<T> <: B<T> {
    init(a: T) {}
    public func foo(x: T): Float64 {
        return .2
    }
}

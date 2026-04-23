// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I1<T> {
    func i1(a: Int32): Unit
    func i1(a: T): Unit
}

interface I2<T> <: I1<T> {}

open class C1<T> <: I1<T> {
    public func i1(a: Int32): Unit {}
    public func i1(a: T): Unit {}
}

interface I3 <: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>I2<Int32><!> {}

class C2 <: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C1<Int32><!> {}

main() {
    var a = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C1<Int32><!>()
    return 0
}

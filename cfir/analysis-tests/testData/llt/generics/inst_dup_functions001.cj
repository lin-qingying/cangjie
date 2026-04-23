// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A<T> {
    public func foo(a: Int32) {}
    public func foo(a: T) {}
}

interface B<T> {
    func foo(a: Int32): Unit
    func foo(a: T): Unit
}

struct C<T> {
    public func foo(a: Int32) {}
    public func foo(a: T) {}
}

main() {
    var a = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>A<Int32><!>()
    var b: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>B<Int32><!>
    var c = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C<Int32><!>()
    return 0
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class A<X, Y, Z> {
    public func test(a: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>A<X, Y, Z><!>, b: A<X, Y, Z>) {}
    public func test(a: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>A<X, Y, Int8><!>, b: A<X, Y, Z>) {}
    public func test(a: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>A<Int8, Int8, Int8><!>, b: A<X, Y, Z>) {}
}

interface B<X, Y, Z> {
    func test(a: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>B<X, Y, Z><!>, b: B<X, Y, Z>) {}
    func test(a: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>B<X, Y, Int8><!>, b: B<X, Y, Z>) {}
    func test(a: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>B<Int8, Int8, Int8><!>, b: B<X, Y, Z>) {}
}

struct C<X, Y, Z> {
    public func test(a: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C<X, Y, Z><!>, b: C<X, Y, Z>) {}
    public func test(a: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C<X, Y, Int8><!>, b: C<X, Y, Z>) {}
    public func test(a: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C<Int8, Int8, Int8><!>, b: C<X, Y, Z>) {}
}

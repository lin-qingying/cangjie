// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class C<A> {
    func f<X>(a: A, b: X) {}
    static func g<X>(a: A, b: X) {}
}

extend<B> C<B> {
    func <!EXTEND_MEMBER_CANNOT_SHADOW!>f<!><Y>(a: B, b: Y) {}
    static func <!EXTEND_MEMBER_CANNOT_SHADOW!>g<!><Y>(c: B, d: Y) {}
}

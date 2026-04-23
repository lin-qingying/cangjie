// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I {
    static func f1(): Unit
}

abstract class <!INHERIT_ABSTRACT_CLASS_STATIC_UNIMPLEMENT_FUNC!>A<!> <: I {}

func f<T>(): Unit where T <: A {
    T.f1()
}

main() {
    f<A>()
}

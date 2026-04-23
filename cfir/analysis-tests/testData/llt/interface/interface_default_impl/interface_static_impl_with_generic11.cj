// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface Ser<T> {
    static func foo(): Unit
}

class AL<T> {}

extend<T> AL<T> <: Ser<AL<T>> where T <: Ser<T> {
    public static func foo(): Unit {}
}

abstract class <!INHERIT_ABSTRACT_CLASS_STATIC_UNIMPLEMENT_FUNC!>DM<!> <: Ser<DM> {}

main() {
    var a = AL<DM>()
}

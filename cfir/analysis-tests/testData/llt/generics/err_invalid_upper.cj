// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DIAGNOSTICS: -UNUSED_VARIABLE

class Aoo<T> where T <: <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>Bool<!> & Object {
    public var a: T
    public init(x: T) {
        a = x
    }
}

main() {
    var a = <!GENERIC_ARGUMENT_NO_MATCH!>Aoo<Bool><!>(false)
    if (!a.a) {
        return 1
    }
    return 0
}

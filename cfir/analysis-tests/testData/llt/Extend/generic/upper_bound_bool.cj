// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class C<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>, <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>V<!>> where T <: Bool, V <: Bool {
    func f(a: T) {
        return a && true
    }
    func g(a: T) {
        return a || false
    }
    func g(a: T, b: V) {
        return a && b
    }
    func g(a: T, b: Bool, c: V) {
        return false || a || false || b || false || c || false
    }
}

main() {
    var c = C<Bool, Bool>()
    if (c.f(false)) {
        return 1
    }
    if (!c.g(true)) {
        return 2
    }
    if (!c.g(true, true) && c.g(true, false)) {
        return 3
    }
    if (!c.g(true, false, false)) {
        return 4
    }
    return 0
}

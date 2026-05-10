// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

interface I<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>, R> where T <: R {
    func foo(): R
}

class C<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>, R> <: I<T, R> where T <: R {
    var x: T
    init(_x: T) {
        x = _x
    }
    public func <!OVERRIDING_RETURN_TYPE_MISMATCH!>foo<!>(): T {
        x
    }
}

main() {
    0
}

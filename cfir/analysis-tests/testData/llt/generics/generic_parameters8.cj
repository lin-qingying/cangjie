// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

main(): Int64 {
    if (B<String, Int64>().m != 1) {
        return 1
    }
    return 0
}

interface I<T> {
    func foo(): Int64 {
        return 1
    }
}

interface <!INTERFACE_CANNOT_INHERIT_CLASS!>J<!><<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>, Y> <: I<T> & I<<!UNRESOLVED_REFERENCE!>V<!>> where T <: String, <!UNRESOLVED_REFERENCE!>V<!> <: Int64 {
    prop m: Int64 {
        get() {
            return foo()
        }
    }
}

class B<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>, <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>V<!>> <: <!GENERIC_ARGUMENT_NO_MATCH!>J<T><!> where T <: String, V <: Int64 {}

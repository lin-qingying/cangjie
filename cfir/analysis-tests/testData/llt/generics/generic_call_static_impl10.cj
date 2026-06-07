// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


// Test for fuzzing CheckInstTypeCompleteness
interface I0<T> {}

interface <!INTERFACE_CANNOT_INHERIT_CLASS!>I1<!> <: I0<Int64> & Option<<!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>I0<!>> {
    static func foo() {}
}

func test<T>() where T <: I1 {
    T.foo()
}

main() {
    test<I1>()
    return 0
}

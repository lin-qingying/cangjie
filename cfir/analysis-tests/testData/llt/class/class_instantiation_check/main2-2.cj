// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %noColor %f 2>&1 | compare %f

interface I1<T> {
    func i1(a: Int32): Unit
    func i1(a: T): Unit
}

interface I2 <: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>I1<Int32><!> {}

interface I3 <: I1<Int64> {}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C1<!> <: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>I1<Int32><!> {}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C2<!> <: I1<Int64> {}

main() {
    return 0
}

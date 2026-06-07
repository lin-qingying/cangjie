// FILE: pkga/pkga.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkga

public interface I1<T> {
    func i1(a: Int32): Unit
    func i1(a: T): Unit
}

// FILE: main.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


import pkga.I1

interface I2 <: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>I1<Int32><!> {}

interface I3<T> <: I1<T> {}

interface I4 <: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>I3<Int32><!> {}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C1<!> <: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>I1<Int32><!> {}

open <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C2<T><!> <: I1<T> {}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C3<!> <: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>I3<Int32><!> {}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C4<!> <: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C2<Int32><!> {}

main() {
    var a = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C2<Int32><!>()
    return 0
}

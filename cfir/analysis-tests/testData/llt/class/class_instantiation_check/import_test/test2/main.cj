// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkga

import pkga.C

class C1 <: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C<Int32><!> {}

class C4 <: C<Int64> {}

open class C2<T> <: C<T> {}

class C3 <: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C2<Int32><!> {}

class C5 <: C2<Int64> {}

main() {
    var a1 = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C<Int32><!>()
    var a2 = C<Int64>()
    var b1 = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C2<Int32><!>()
    var b2 = C2<Int64>()
    return 0
}

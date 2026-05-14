// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %diag_json %f -o %output 2>&1 | compare %f

interface M0_<T> {}

interface M1_<T> {}

interface M6_<T> {}

interface M7_<T> {}

func test<T0_, T1_>(i: ((T0_) -> T1_)) where T0_ <: M0_<T1_>, T1_ <: M6_<<!UNRESOLVED_REFERENCE!>T3_<!>>, <!UNRESOLVED_REFERENCE!>T3_<!> <: M1_<<!UNRESOLVED_REFERENCE!>T3_<!>> {
    0
}

interface I0_ <: M0_<I1_> {}

interface I1_ <: M6_<I3_> {}

interface I3_ <: M1_<I3_> {}

open class G1_ <: I1_ {}

main() {
    <!UNABLE_TO_INFER_GENERIC_FUNC!>test({i: M0_<I1_> => G1_()})<!>
}

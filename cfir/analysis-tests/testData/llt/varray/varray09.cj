// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

interface I {}

extend Int64 <: I {}

main() {
    let a1_1: Array<Int64> = <!TYPE_MISMATCH!>VArray<Int64, $3>()<!> {i => i} // x2 error
    let a1_2: Array<Int64> = <!TYPE_MISMATCH!>VArray<Int64, $3><!> {i => i} // x2 error

    let a2_1: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>I<!>, $3> = <!TYPE_MISMATCH!>VArray<Int64, $3>()<!> {i => i} // x2 error
    let a2_2: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>I<!>, $3> = <!TYPE_MISMATCH!>VArray<Int64, $3><!> {i => i} // x2 error

    let a3_1: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>Any<!>, $3> = <!TYPE_MISMATCH!>VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>I<!>, $3>()<!> {i => i} // x2 error
    let a3_2: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>Any<!>, $3> = <!TYPE_MISMATCH!>VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>I<!>, $3><!> {i => i} // x2 error
    0
}

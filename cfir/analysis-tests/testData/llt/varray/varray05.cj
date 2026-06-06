// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

main() {
    let a1: VArray<!parse_varray_type_parameter!> <!>= VArray<Int64, $3>(repeat: 0) // x2 error
    let a2: VArray<!parse_varray_type_args_mismatch!><Int64><!> = VArray<Int64, $3>(repeat: 0) // x2 error
    let a3: VArray<<!parse_expected_type!>$<!>3> = VArray<Int64, $3>(repeat: 0) // x2 error

    let a4: VArray<Int64, $3>= VArray<!parse_varray_type_parameter!>(<!>repeat:0) // x2 error
    let a5: VArray<Int64, $3>= VArray<!parse_varray_type_args_mismatch!><Int64><!>(repeat:0) // x2 error
    let a6: VArray<Int64, $3>= VArray<<!parse_expected_type!>$<!>3>(repeat:0) // x2 error
    0
}

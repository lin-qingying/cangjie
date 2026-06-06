// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

main() {
    let a1: VArray<!parse_varray_type_parameter!> <!>= [1, 2, 3] // error
    let a2: VArray<<!parse_expected_type!>$<!>1> = [1, 2, 3] //error
    let a3: VArray<!parse_varray_type_args_mismatch!><Int64><!> = [1, 2, 3] //error
    let a4: VArray<<!parse_expected_type!>$<!>3, Int64> = [1, 2, 3] //error
    let a5: VArray<<!parse_expected_type!>$<!>3, Int64, Int64> = [1, 2, 3] //error
    let a6: VArray<Int64, <!parse_varray_type_args_mismatch!>Int64<!>> = [1, 2, 3] //error
    0
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main() {
    let a1_1 = VArray<Int64, $3><!VARRAY_ARGS_NUMBER_MISMATCH!>(repeat: 1, {i => i})<!> // x2 error
    let a1_2 = VArray<Int64, $3><!VARRAY_ARGS_NUMBER_MISMATCH!>()<!> // x2 error
    let a1_3: VArray<Int64, $3> = VArray<Int64, $3><!VARRAY_ARGS_NUMBER_MISMATCH!>()<!> // error
    let a1_4 = VArray<<!UNRESOLVED_REFERENCE!>C<!>, $3>(repeat: 0) // error
    0
}

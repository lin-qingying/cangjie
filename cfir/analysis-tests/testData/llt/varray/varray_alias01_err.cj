// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f  -o %output 2>&1 | compare %f

type varr = VArray<Rune, $3>

<!typealias_unused_type_parameters!>t<!>ype varr1_1<T> = VArray<Int64, $3>

// Omit Generic Arguments.

main() {
    var a: varr = varr(repeat: <!sema_cannot_convert_literal!>0<!>)

    var c: <!sema_generic_type_without_type_argument!>v<!>arr1_1 = varr1_1(repeat: 0) // error x2
}

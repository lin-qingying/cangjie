// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main() {
    // Get and Set of Multidimensional Arrays.
    var arr8: VArray<VArray<Int64, $2>, $2> = [[1, 2], [3, 4]]
    let element1 = arr8[1][0] // ok
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>arr8[1][1] = 5<!> // error
    let element1_1 = <!BUILTIN_INDEX_IN_BOUND!>arr8<!>[2][1] // error
    <!CANNOT_ASSIGN_TO_IMMUTABLE!><!BUILTIN_INDEX_IN_BOUND!>arr8[1]<!>[2] = 6<!> // error
}

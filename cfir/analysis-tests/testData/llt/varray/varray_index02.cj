// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

main() {
    // Get and Set of Multidimensional Arrays.
    var arr8: VArray<VArray<Int64, $2>, $2> = [[1, 2], [3, 4]]
    let element1 = arr8[1][0] // ok
    <!sema_cannot_assign_to_immutable!>arr8[1][1] = 5<!> // error
    let element1_1 = <!sema_builtin_index_in_bound!>a<!>rr8[2][1] // error
    <!sema_builtin_index_in_bound!><!sema_cannot_assign_to_immutable!>a<!>rr8[1][2] = 6<!> // error
}

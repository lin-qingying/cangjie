// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

main(): Int64 {
    var myArray1: Array<Int32>
    <!sema_empty_arrayLit_type_undefined!>[<!>]
    var myArray2: Array<Array<Int32>> = [[]]
    return 0
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

main(): Int64 {
    let arr = [2, 1, 0]
    arr[1..2] = arr[0..1]
    <!sema_cannot_assign_to_subscript!>arr[1..2]<!> += arr[0..1]
    return 0
}

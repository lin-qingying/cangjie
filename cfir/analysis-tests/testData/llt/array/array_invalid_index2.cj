// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

main(): Int64 {
    let arr = [2, 1, 0]
    arr[1..2] = arr[0..1] // EXPECTED: regex-auto type Array<Int64> does not have operator func [](index, value) for index type
    arr[1..2] += arr[0..1] // EXPECTED: regex-auto invalid binary operator '+' on type 'Array<Int64>' and 'Array<Int64>'
    return 0
}

// ASSERT: regex :5:.* type Array<Int64> does not have operator func [](index, value) for index type Struct-RangeInt64>
// ASSERT: regex :6:.* type incompatible in this compound assignment expression

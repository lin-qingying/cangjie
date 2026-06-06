// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %cmp_opt %typecheck %cmp_opt %f 2>&1 | compare %f

main() : Int64 {
    let <!parse_tuple_pattern_expected_more_field!>(m)<!> = (1)
    let n = match (m) {
        case <!parse_tuple_pattern_expected_more_field!>(p)<!> => p
    }
    var a: (Int32,Int32,Int32,Int32) = (1, 2, 3, 4)
    var b = (8, a[5], 9)
    return b[1]
}

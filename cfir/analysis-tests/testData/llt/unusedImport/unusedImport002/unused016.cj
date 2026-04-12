// EXEC-PIPE: %compiler %cmp_opt  %f -o %output 2>&1 | compare %f

package c
import std.convert.Parsable

main() {
    println(UInt32.parse("123"))
    return 0
}

/* SCAN
*/


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

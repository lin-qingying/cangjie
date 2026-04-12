// DEPENDENCE: ./b.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt b.cj -o b.%middle
// EXEC-PIPE: %compiler %cmp_opt b.%middle %f -o %output 2>&1 | compare %f

package c
import b.*

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

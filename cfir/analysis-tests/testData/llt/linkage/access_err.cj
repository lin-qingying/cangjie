// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// LEVEL: 0
// DEPENDENCE: base01.cj %n.cjnative.expect
// EXEC: %compiler %cmp_opt %compile_lib_opt base01.cj -o base01.%middle
// (CJNATIVE) EXEC: %compiler %cmp_opt base01.%middle %f -o %output 2>&1 | compare %n.cjnative.expect

package a.b.c

import a.b.*

main() {
    println(DDDD().f4())
    println(EEEE().f4())
    println(FFFF().f4())
}

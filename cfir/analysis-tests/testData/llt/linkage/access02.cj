// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: base02.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt base02.cj -o base02.%middle
// EXEC: %compiler %cmp_opt base02.%middle %f -o %output
// EXEC: %run %run_opt %output | compare %f
// ASSERT: scan-3 1

package a.b.c

import a.b.*

main() {
    println(GGGG().f1())
    println(HHHH().f1())
    println(IIII().f1())
}

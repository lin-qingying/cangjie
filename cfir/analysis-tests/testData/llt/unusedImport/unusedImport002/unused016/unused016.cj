// DEPENDENCE: ./a.cj ./b.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt a.cj -o a.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt b.cj -o b.%middle
// EXEC-PIPE: %compiler %cmp_opt b.%middle a.%middle %f -o %output 2>&1 | compare %f

package c
import a.X
import a.I
import b.A

main() {
   var a = Array<A>()
   a.testFunc()
   return 0
}

/* SCAN
*/


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

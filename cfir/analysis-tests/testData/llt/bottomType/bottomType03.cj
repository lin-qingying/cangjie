// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

let one: Int32 = 1
let two: Int32 = 2

func foo() {
    return 1
    let a = one
    let b = two
    return a + b
}

main(): Int64 {
    foo()
    return 0
}

/* SCAN
error: the return type of this function cannot be calculated from the function body and all the return expressions
  ==> bottomType03.cj:16:5:
   | 
16 |     return a + b
   |     ^ 
   | 
   # note: Traces:
The types 'Int32', 'Int64' and 'Nothing' do not have the smallest common supertype


1 error generated, 1 error printed.
*/

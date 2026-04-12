// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func foo(a!: Int32 = 1, b!: Int32 = 1) {
    return a + b
}

main() {
    var x = foo
    var res = x(a: 2, b: 2)
    if (res != 4) {
        return 1
    }
    return 0
}

/* SCAN
error: named argument cannot be used in variable function call
  ==> defaultParameter4_2.cj:15:17:
   | 
15 |     var res = x(a: 2, b: 2)
   |                 ^ 
   | 

error: named argument cannot be used in variable function call
  ==> defaultParameter4_2.cj:15:23:
   | 
15 |     var res = x(a: 2, b: 2)
   |                       ^ 
   | 

2 errors generated, 2 errors printed.
*/

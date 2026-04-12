// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func foo1(): Int32 {
    1
}

func foo2(): Int32 {
    2
}

main(): Int64 {
    foo1 = foo2
    return 0
}

/* SCAN
error: 'func' can not be assigned
  ==> assign_001.cj:18:5:
   | 
18 |     foo1 = foo2
   |     ^^^^ 
   | 

1 error generated, 1 error printed.
*/

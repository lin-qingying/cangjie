// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class A {
    func a() {}
    var c = this.a
}

/* SCAN
error: 'a' is not allowed to be accessed before all member variables are initialized
  ==> member1.cj:11:13:
   | 
11 |     var c = this.a
   |             ^ 
   | 

1 error generated, 1 error printed.
*/

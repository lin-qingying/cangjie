// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

class D {
    public var a: Int32 = a
}

main() {
    return 0
}

/* SCAN
error: undeclared identifier 'a'
  ==> class12.cj:10:27:
   | 
10 |     public var a: Int32 = a
   |                           ^ 
   | 

1 error generated, 1 error printed.
*/

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

class A {
    public init() {
        return Int32 
    }
}

/* SCAN
error: expected expression after keyword 'return', found keyword 'Int32'
  ==> class_init_with_return3.cj:11:16:
   | 
11 |         return Int32 
   |                ^^^^^ expected expression here
   | 

1 error generated, 1 error printed.
*/

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

interface A {}

extend A <: A {}

main() {
    0
}

/* SCAN
error: extending type 'Interface-A' is not allowed
  ==> extend_inheritance_circle1.cj:11:8:
   | 
11 | extend A <: A {}
   |        ^ 
   | 

1 error generated, 1 error printed.
*/

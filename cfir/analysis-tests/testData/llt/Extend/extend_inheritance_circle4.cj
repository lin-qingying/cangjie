// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

class D {}

interface A <: B {}

interface C <: A {}

interface B <: C {}

extend D <: C {}

main() {
    0
}

/* SCAN
error: inheritance cycle detected: 'C->A->B->C'
  ==> extend_inheritance_circle4.cj:17:1:
   | 
17 | extend D <: C {}
   | ^ 
   | 

1 error generated, 1 error printed.
*/

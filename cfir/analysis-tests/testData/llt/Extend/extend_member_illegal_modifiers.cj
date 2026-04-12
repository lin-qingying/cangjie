// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class A { }

extend A {
    protected func foo() { } 
    public open func boo() { } 

}

/* SCAN
error: unexpected modifier 'open' on function declaration in extend body
  ==> extend_member_illegal_modifiers.cj:13:12:
   | 
11 | extend A {
   | ~~~~~~ in extend
12 |     protected func foo() { } 
13 |     public open func boo() { } 
   |            ^^^^ ~~~~ on function declaration
   |            |
   |            unexpected modifier
   | 

1 error generated, 1 error printed.
*/

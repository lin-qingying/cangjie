// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

package pkgs

public class A{
    public var a: Int64
    public var b: Int64
    public init(x: Int64){
        a = x
        b = x
    }
    publ (index > 1) {
            3
        } else {
            this.a
        }
    }
ic operator func [](index: Int64): Int64{
        if (index > 1) {
            3
        } else {
            this.a
        }
    }
    public operator func []( index: Int64, value!: Int64): Unit{
        this.b = value
    }
}

/* SCAN
error: expected declaration, found 'publ'
  ==> broken_index_overload.cj:18:5:
   | 
18 |     publ (index > 1) {
   |     ^^^^ expected declaration here
   | 

error: expected declaration, found 'ic'
  ==> broken_index_overload.cj:24:1:
   | 
24 | ic operator func [](index: Int64): Int64{
   | ^^ expected declaration here
   | 
   # note: only declarations or macro expressions can be used in the top-level

error: expected ';' or '<NL>', found keyword 'operator'
  ==> broken_index_overload.cj:24:4:
   | 
24 | ic operator func [](index: Int64): Int64{
   |    ^^^^^^^^ expected ';' or '<NL>' here
   | 

error: unexpected modifier 'operator' on function declaration in 'top-level' scope
  ==> broken_index_overload.cj:31:12:
   | 
31 |     public operator func []( index: Int64, value!: Int64): Unit{
   |            ^^^^^^^^ ~~~~ on function declaration in 'top-level' scope
   |            |
   |            unexpected modifier
   | 

error: expected declaration, found '}'
  ==> broken_index_overload.cj:34:1:
   | 
34 | }
   | ^ expected declaration here
   | 
   # note: only declarations or macro expressions can be used in the top-level

5 errors generated, 5 errors printed.
*/

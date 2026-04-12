// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class A {}

interface Foo {
    prop v1: Int64
}

extend A <: Foo {}

main() {
    0
}

/* SCAN
error: implementation of function or property is needed in 'extend A'
  ==> extend_property6.cj:15:1:
   | 
15 | extend A <: Foo {}
   | ^ 
   | 
note: unimplemented interface property 'v1'
  ==> extend_property6.cj:12:5:
   | 
12 |     prop v1: Int64
   |     ^ 
   | 

1 error generated, 1 error printed.
*/

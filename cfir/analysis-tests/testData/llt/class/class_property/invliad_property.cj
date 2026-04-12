// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

enum A{
    a(Int64,Int64,Int64)
    public prop : Int64{
       get(v){1}
       set(v){ }
    }
}

main(): Int64 {
    var i = A.a(1,2,3)
    return i.a - 1
}

/* SCAN
error: expected a property name after keyword 'prop' in property definition, found ':'
  ==> invliad_property.cj:11:17:
   | 
11 |     public prop : Int64{
   |            ~~~~ ^ expected a property name here
   |            |
   |            after keyword 'prop' in property definition
   | 

1 error generated, 1 error printed.
*/

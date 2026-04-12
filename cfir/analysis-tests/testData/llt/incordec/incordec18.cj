// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f


main():Int64 {
    var a  = {x:Int64=>x}(1)++ 
    return 0
}

/* SCAN
error: cannot increment a un-assignable expression
  ==> incordec18.cj:11:29:
   | 
11 |     var a  = {x:Int64=>x}(1)++ 
   |              ~~~~~~~~~~~~~~~^^ 
   |              |
   |              cannot assign to this expression
   | 

1 error generated, 1 error printed.
*/

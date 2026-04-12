// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

struct S {
    const init
}
main() {}

/* SCAN
error: body of constructor is missing
  ==> err_invalid_ctor.cj:10:15:
   | 
10 |     const init
   |               ^ missing constructor body here
   | 

error: expected '(' or '<', found '}'
  ==> err_invalid_ctor.cj:11:1:
   | 
11 | }
   | ^ expected '(' or '<' here
   | 

2 errors generated, 2 errors printed.
*/

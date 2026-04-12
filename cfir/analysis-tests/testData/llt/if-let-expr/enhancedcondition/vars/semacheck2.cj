// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %f %cmp_opt 2>&1 | compare %f

main() {
    if (let ) {

    }
    while (let  <-) {

    }

    if (let a) {
        
    }
}

/* SCAN
error: expected pattern, found ')'
  ==> semacheck2.cj:10:13:
   | 
10 |     if (let ) {
   |             ^ expected pattern here
   | 

error: expected pattern, found '<-'
  ==> semacheck2.cj:13:17:
   | 
13 |     while (let  <-) {
   |                 ^^ expected pattern here
   | 

error: expected expression after '<-', found ')'
  ==> semacheck2.cj:13:19:
   | 
13 |     while (let  <-) {
   |                   ^ expected expression here
   | 

error: expected '<-' in if-let expression, found ')'
  ==> semacheck2.cj:17:14:
   | 
17 |     if (let a) {
   |              ^ expected '<-' here
   | 

4 errors generated, 4 errors printed.
*/

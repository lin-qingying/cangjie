// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    try {
    } catch (e: Exception) {
        let e = 0
        println(e)
    }
}

/* SCAN
error: redefinition of declaration 'e'
  ==> err_scope_00.cj:12:13:
   | 
12 |         let e = 0
   |             ^ 
   | 
note: 'e' is previously declared here
  ==> err_scope_00.cj:11:14:
   | 
11 |     } catch (e: Exception) {
   |              ^ 
   | 

1 error generated, 1 error printed.
*/

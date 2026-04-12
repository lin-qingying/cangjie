// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    let xx = "unused"
    for (xx in [1, 2]) {
        println(xx)
        var xx = false
        println(xx)
    }
}

/* SCAN
error: redefinition of declaration 'xx'
  ==> for_in_00.cj:13:13:
   | 
13 |         var xx = false
   |             ^^ 
   | 
note: 'xx' is previously declared here
  ==> for_in_00.cj:11:10:
   | 
11 |     for (xx in [1, 2]) {
   |          ^^ 
   | 

1 error generated, 1 error printed.
*/

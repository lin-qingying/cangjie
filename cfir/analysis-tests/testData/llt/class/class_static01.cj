// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

class Data {
    public static let a = 1
    public var b = 2
1}  

main(): Int64 {
    Data().b
    Data.a
    return 0
}

/* SCAN
error: expected declaration, found literal '1'
  ==> class_static01.cj:12:1:
   | 
12 | 1}  
   | ^ expected declaration here
   | 

1 error generated, 1 error printed.
*/

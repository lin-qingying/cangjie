// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f

main() {
    let a: Array<UInt8> = Array<UInt8>(1, repeat: 0)
    a[match (0) { case x => 1; case _ => 0 }] *= match (0) { case x =>  }
}

/* SCAN
error: match case cannot be empty
  ==> invalid_enum_pattern_003.cj:11:71:
   | 
11 |     a[match (0) { case x => 1; case _ => 0 }] *= match (0) { case x =>  }
   |                                                              ~~~~     ^ match case cannot be empty
   | 

1 error generated, 1 error printed.
*/

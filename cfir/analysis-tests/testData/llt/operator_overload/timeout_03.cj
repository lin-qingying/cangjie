// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output
// TIMEOUT: 10

main() {
    let (a, b) = (unde, fined)
    let _ = ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a ==
        ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == b) && (a == true))))))))))))))))))))))))))))))))))))))))))))))))))))
}

/* SCAN
error: undeclared identifier 'unde'
  ==> timeout_03.cj:11:19:
   | 
11 |     let (a, b) = (unde, fined)
   |                   ^ 
   | 

error: undeclared identifier 'fined'
  ==> timeout_03.cj:11:25:
   | 
11 |     let (a, b) = (unde, fined)
   |                         ^ 
   | 

2 errors generated, 2 errors printed.
*/

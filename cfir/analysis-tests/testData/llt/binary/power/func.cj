// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f %cmp_opt 2>&1 | compare %f

func foo() {}
main() {
    let a = foo 
    let b = foo 
    let c = (1,2)
    let d = (3,4)
    a==b 
    c + d 
    0
}
/* SCAN
error: invalid binary operator '==' on type '() -> Unit' and '() -> Unit'
  ==> func.cj:15:6:
   | 
15 |     a==b 
   |      ^^ 
   | 

error: invalid binary operator '+' on type 'Tuple<Int64, Int64>' and 'Tuple<Int64, Int64>'
  ==> func.cj:16:7:
   | 
16 |     c + d 
   |       ^ 
   | 

2 errors generated, 2 errors printed.
*/

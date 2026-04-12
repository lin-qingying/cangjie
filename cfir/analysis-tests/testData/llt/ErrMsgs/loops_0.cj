// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %compile_lib_opt %n.cj 2>&1 | compare %f



main() {
    let a = 1 + true

    while (a) {
        let b = 1 + false
    }

    do {
        let b = 1 + false
    } while (a)

    for (x in [1] where a) {
        let b = 1 + false
    }

    for (a in a) {}

    0
}

// No errors:





// Errors:

/* SCAN
error: invalid binary operator '+' on type 'Int64' and 'Bool'
  ==> loops_0.cj:12:15:
   | 
12 |     let a = 1 + true
   |               ^ 
   | 
   # note: you may want to implement 'operator func +(right: Bool)' for type 'Int64'

error: invalid binary operator '+' on type 'Int64' and 'Bool'
  ==> loops_0.cj:15:19:
   | 
15 |         let b = 1 + false
   |                   ^ 
   | 
   # note: you may want to implement 'operator func +(right: Bool)' for type 'Int64'

error: invalid binary operator '+' on type 'Int64' and 'Bool'
  ==> loops_0.cj:19:19:
   | 
19 |         let b = 1 + false
   |                   ^ 
   | 
   # note: you may want to implement 'operator func +(right: Bool)' for type 'Int64'

error: invalid binary operator '+' on type 'Int64' and 'Bool'
  ==> loops_0.cj:23:19:
   | 
23 |         let b = 1 + false
   |                   ^ 
   | 
   # note: you may want to implement 'operator func +(right: Bool)' for type 'Int64'

4 errors generated, 4 errors printed.
*/

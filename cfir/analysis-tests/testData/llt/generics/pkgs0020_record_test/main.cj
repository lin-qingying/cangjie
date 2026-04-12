// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkgs
// EXEC: %compiler %cmp_opt %compile_lib_opt pkgs/pkgs.cj -o pkgs/pkgs.%middle
// EXEC-PIPE-1: %compiler %cmp_opt pkgs/pkgs.%middle %n.cj --import-path pkgs 2>&1 | compare %f

import pkgs.*

main(): Int64 {
    return run()
}

func run(): Int64 {
    var res: Int32
    var a: Int32 = 3
    var b: Int32 = 4
    let data: Data<Int32> = Data<Int32>(3, 4)
    let data2: Data<Int32> = Data<Int32>(3, 4)
    res = data.area()
    if (res == 3) {
        return 0
    }
    return 1
}

/* SCAN
error: undeclared type name 'Data'
  ==> main.cj:21:15:
   | 
21 |     let data: Data<Int32> = Data<Int32>(3, 4)
   |               ^ 
   | 

error: undeclared type name 'Data'
  ==> main.cj:22:16:
   | 
22 |     let data2: Data<Int32> = Data<Int32>(3, 4)
   |                ^ 
   | 

error: undeclared identifier 'Data'
  ==> main.cj:21:29:
   | 
21 |     let data: Data<Int32> = Data<Int32>(3, 4)
   |                             ^ 
   | 

error: undeclared identifier 'Data'
  ==> main.cj:22:30:
   | 
22 |     let data2: Data<Int32> = Data<Int32>(3, 4)
   |                              ^ 
   | 

warning: unused import 'pkgs.*'
  ==> main.cj:11:1:
   | 
11 | import pkgs.*
   | ^^^^^^^^^^^^^ unused import
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

4 errors generated, 4 errors printed.
1 warning generated, 1 warning printed.
*/

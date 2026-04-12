// DEPENDENCE: ./a.cj ./b.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt a.cj -o a.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt b.cj -o b.%middle
// EXEC-PIPE: %compiler %cmp_opt b.%middle a.%middle %f -o %output 2>&1 | compare %f

package c
import b.MyClass

main() {
    var myClassImpl = MyClass()
}

/* SCAN
warning: unused variable:'myClassImpl'
  ==> unused013.cj:10:9:
   | 
10 |     var myClassImpl = MyClass()
   |         ^^^^^^^^^^^ unused variable
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

1 warning generated, 1 warning printed.
*/


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// LEVEL: 0
// DEPENDENCE: ./aux_extend_test_a03_01.cj ./aux_test_a03_01.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt aux_test_a03_01.cj -o b.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt aux_extend_test_a03_01.cj -o a.%middle
// (not CJNATIVE) EXEC-1: %compiler %cmp_opt b.%middle a.%middle %f -o %output
// (CJNATIVE) ERRCHECK: %compiler %cmp_opt b.%middle a.%middle %f -o %output

package c

import b.{MyClass,I1}

main() {
    var myClassImpl = MyClass()
    myClassImpl.test1()
    myClassImpl.test2()
}

/* SCAN
error: no matching function declaration for function call 'test2'
  ==> test_a03_03.cj:15:17:
   | 
15 |     myClassImpl.test2()
   |                 ^ 
   | 

1 error generated, 1 error printed.
*/

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

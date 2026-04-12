// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %noColor %cmp_opt_chir2hlir %f -o %n.%suffix 2>&1 | compare %f

func foo(): CString {
    return  {}
}

main() {
    var c: CString =  {}
}

/* SCAN
error: expected '=>' in lambda expression, found '}'
  ==> blockexpr7.cj:10:14:
   | 
10 |     return  {}
   |              ^ expected '=>' here
   | 

error: expected '=>' in lambda expression, found '}'
  ==> blockexpr7.cj:14:24:
   | 
14 |     var c: CString =  {}
   |                        ^ expected '=>' here
   | 

2 errors generated, 2 errors printed.
*/

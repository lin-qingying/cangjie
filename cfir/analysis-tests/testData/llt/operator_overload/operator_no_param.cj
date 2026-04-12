// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

struct A<T>{
    public operator func >=(:A<T>):Int16 {return 0}
}

main(): Int64 {
    return 0
}

/* SCAN
error: expected a argument name after '(' in parameter list, found ':'
  ==> operator_no_param.cj:10:29:
   | 
10 |     public operator func >=(:A<T>):Int16 {return 0}
   |                            ~^ expected a argument name here
   |                            |
   |                            after '(' in parameter list
   | 

error: body of function '>=' is missing
  ==> operator_no_param.cj:10:34:
   | 
10 |     public operator func >=(:A<T>):Int16 {return 0}
   |                                  ^ missing function body here
   | 

2 errors generated, 2 errors printed.
*/

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %f %cmp_opt

func f1() {
    @IfAvailable(APILevel:11, { => println(0) })
}

func f2() {
    @IfAvailable(APILevel:11, )
}

func f3() {
    @IfAvailable()
}

main() {
}

/* SCAN
error: expected ',', found ')'
  ==> err_too_few_args.cj:10:48:
   | 
10 |     @IfAvailable(APILevel:11, { => println(0) })
   |                                                ^ expected ',' here
   | 

error: @IfAvailable expect a literal lambda here
  ==> err_too_few_args.cj:14:31:
   | 
14 |     @IfAvailable(APILevel:11, )
   |                               ^ 
   | 

error: expected expression after ',', found ')'
  ==> err_too_few_args.cj:14:31:
   | 
14 |     @IfAvailable(APILevel:11, )
   |                               ^ expected expression here
   | 

error: expected expression after '(', found ')'
  ==> err_too_few_args.cj:18:18:
   | 
18 |     @IfAvailable()
   |                  ^ expected expression here
   | 

4 errors generated, 4 errors printed.
*/

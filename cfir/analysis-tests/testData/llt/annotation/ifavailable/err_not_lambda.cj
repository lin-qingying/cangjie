// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %f %cmp_opt

func mem() {}

main() {
    @IfAvailable(APILevel:11, mem, mem)
}

/* SCAN
error: @IfAvailable expect a literal lambda here
  ==> err_not_lambda.cj:12:31:
   | 
12 |     @IfAvailable(APILevel:11, mem, mem)
   |                               ^^^ 
   | 

error: @IfAvailable expect a literal lambda here
  ==> err_not_lambda.cj:12:36:
   | 
12 |     @IfAvailable(APILevel:11, mem, mem)
   |                                    ^^^ 
   | 

2 errors generated, 2 errors printed.
*/

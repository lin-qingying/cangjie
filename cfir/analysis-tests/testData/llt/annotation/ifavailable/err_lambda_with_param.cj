// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %f %cmp_opt

main() {
    @IfAvailable(Level:11, {=>}, { a:Int=>() })
}

/* SCAN
error: unknown parameter name 'Level'
  ==> err_lambda_with_param.cj:10:18:
   | 
10 |     @IfAvailable(Level:11, {=>}, { a:Int=>() })
   |                  ^^^^^ 
   | 

error: mismatched types
  ==> err_lambda_with_param.cj:10:34:
   | 
10 |     @IfAvailable(Level:11, {=>}, { a:Int=>() })
   |                                  ^^^^^^^^^^^^^ expected '() -> Unit', found '(Int64) -> Unit'
   | 

2 errors generated, 2 errors printed.
*/

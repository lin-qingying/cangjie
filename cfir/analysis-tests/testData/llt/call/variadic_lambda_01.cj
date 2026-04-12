// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

let f = {_: Array<Int64>, _: Array<Int64>, _: Array<Int64> =>}

main() {
    var _ = f(f)
}

/* SCAN
error: missing arguments for parameter list '(Struct-Array<Int64>, Struct-Array<Int64>, Struct-Array<Int64>)' in call
  ==> variadic_lambda_01.cj:12:14:
   | 
12 |     var _ = f(f)
   |              ^^^ expected 3 arguments, found 1
   | 
note: found candidate
 ==> variadic_lambda_01.cj:9:5:
  | 
9 | let f = {_: Array<Int64>, _: Array<Int64>, _: Array<Int64> =>}
  |     ^ 
  | 

1 error generated, 1 error printed.
*/

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func f(x: Array<Int64>) {
    x.size
}

main() {
    f([1, 2], 3)
}

/* SCAN
error: extra argument given for parameter list '(Struct-Array<Int64>)' in call
  ==> variadic_err_num.cj:14:6:
   | 
14 |     f([1, 2], 3)
   |      ^^^^^^^^^^^ expected 1 argument, found 2
   | 
note: found candidate
 ==> variadic_err_num.cj:9:6:
  | 
9 | func f(x: Array<Int64>) {
  |      ^ 
  | 

1 error generated, 1 error printed.
*/

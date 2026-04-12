// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func f<T>(x: Array<T>) {
    x.size
}

main() {
    f<Int64>(1, true)
}

/* SCAN
error: extra argument given for parameter list '(Struct-Array<Generics-T>)' in call
  ==> variadic_err_ty.cj:14:13:
   | 
14 |     f<Int64>(1, true)
   |             ^^^^^^^^^ expected 1 argument, found 2
   | 
note: found candidate
 ==> variadic_err_ty.cj:9:6:
  | 
9 | func f<T>(x: Array<T>) {
  |      ^ 
  | 

1 error generated, 1 error printed.
*/

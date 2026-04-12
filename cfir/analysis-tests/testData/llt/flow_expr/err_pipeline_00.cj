// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func f(x: Array<Int>) {
    x
}

main(): Int {
    0 |> f
}

/* SCAN
error: mismatched types
  ==> err_pipeline_00.cj:14:5:
   | 
14 |     0 |> f
   |     ^^^^^^ expected 'Int64', found 'Struct-Array<Int64>'
   | 

1 error generated, 1 error printed.
*/

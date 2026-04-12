// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

let f = {x: String => x}

func f2<T, R>(x: T) {
    {x: R => x}
}

func f3(x: (String) -> Any) {
    x("s")
}

main() {
    let v: Any = f2("s")("s")
    f3(f)
    ()
}

/* SCAN
error: mismatched types
  ==> call_inference_11.cj:21:8:
   | 
21 |     f3(f)
   |        ^ expected '(Struct-String) -> Interface-Any', found '(Struct-String) -> Struct-String'
   | 

1 error generated, 1 error printed.
*/

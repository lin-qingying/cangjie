// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func f(_: Unit, _: Array<Bool>) {
    0
}

main() {
    f(unsafe { let _ = wrong }, [])
}

/* SCAN
error: undeclared identifier 'wrong'
  ==> err_block_00.cj:14:24:
   | 
14 |     f(unsafe { let _ = wrong }, [])
   |                        ^ 
   | 

1 error generated, 1 error printed.
*/

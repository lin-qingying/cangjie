// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %noColor %f -o %output

main() {
    let Int_literal:Int33 = 1suffix;
    return 0;
}

/* SCAN
error: unknown suffix 'suffix' for number literal
  ==> 013.cj:10:30:
   | 
10 |     let Int_literal:Int33 = 1suffix;
   |                              ^^^^^^ unknown suffix 'suffix'
   | 

1 error generated, 1 error printed.
*/

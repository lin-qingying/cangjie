// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

func f<T>(a: T) {}

class G {
    init() {}
    init(a: Int64) {}
}

func func657(): UInt64 {
    var x: Option<UInt64> = Some(1)
    x ?? try {
        UInt64(1)
    } catch (_) {
        [Option<UInt64>.None, Option<UInt64>.NonZ];
        f(G())
        UInt64(2)
    }
}

/* SCAN
error: 'NonZ' is not a member of enum 'Option<UInt64>'
  ==> binaryexpr_and_funcargoverload.cj:21:46:
   | 
21 |         [Option<UInt64>.None, Option<UInt64>.NonZ];
   |                                              ^^^^ 
   | 

error: 'main' is missing
 ==> binaryexpr_and_funcargoverload.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/

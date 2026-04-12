// ERRCHECK: %compiler %enableEH %cmp_opt %f -o %output 2>&1 | compare %f

import stdx.effect.Command

class Get <: Command<Int64> {}

main() {
    try {
        1
    } handle(_: Get) {
        try {
            2
        } catch (_: Exception) {
            "Hello"
        }
    }
}

/* SCAN
error: The type of this catch block is 'Struct-String', which mismatches the smallest common supertype 'Int64' of previous branches.
  ==> mistypedTryCatchHandle.cj:13:32:
   | 
13 |         } catch (_: Exception) {
   |                                ^ 
   | 
note: Traces:
The types 'Int64' and 'Struct-String' do not have the smallest common supertype

  ==> mistypedTryCatchHandle.cj:11:9:
   | 
11 |         try {
   |         ^ 
   | 

1 error generated, 1 error printed.
*/


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

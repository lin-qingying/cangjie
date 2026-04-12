// ERRCHECK: %compiler %enableEH %cmp_opt %f -o %output 2>&1 | compare %f

import stdx.effect.Command

class Get <: Command<Int64> {}

main() {
    try {
        try {
            1
        } catch (_: Exception) {
            println("addakfnh")
        }
    } handle(_: Get) {
        resume with 1
    }
}

/* SCAN
error: The type of this catch block is 'Unit', which mismatches the smallest common supertype 'Int64' of previous branches.
  ==> mistypedTryCatchTryHandle.cj:11:32:
   | 
11 |         } catch (_: Exception) {
   |                                ^ 
   | 
note: Traces:
The types 'Int64' and 'Unit' do not have the smallest common supertype

 ==> mistypedTryCatchTryHandle.cj:9:9:
  | 
9 |         try {
  |         ^ 
  | 

1 error generated, 1 error printed.
*/


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

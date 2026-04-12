// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main() {
    var fdset = FdSetVal()
    fdOne(fdset)
}

@C
public struct FdSetVal {
    var fdBits = VArray<Int64, $6>(repeat: 0)
}

public func fdOne(fdset: FdSetVal): Unit {
    let size: Int64 = fdset.fdBits.size
    for (i in 0..size) {
        fdset.fdBits[i] = 1
        println(fdset.fdBits[i])
    }
}

/* SCAN
error: cannot assign to immutable value
  ==> subsript_with_member_access_let.cj:22:9:
   | 
22 |         fdset.fdBits[i] = 1
   |         ^^^^^^^^^^^^^^^^^^^ 
   | 
note: parameter 'fdset' is immutable
  ==> subsript_with_member_access_let.cj:19:19:
   | 
19 | public func fdOne(fdset: FdSetVal): Unit {
   |                   ^^^^^ 
   | 

1 error generated, 1 error printed.
*/

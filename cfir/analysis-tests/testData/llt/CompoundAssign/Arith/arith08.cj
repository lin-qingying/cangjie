// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

struct Rectangle {
    public var width: Int32 = 1
    public let length: Int32 = 1
    public func set() {
        width += 1
    }
}

main(): Int64 {
    return 0
}

/* SCAN
error: instance member variable 'width' cannot be modified in immutable function
  ==> arith08.cj:13:9:
   | 
13 |         width += 1
   |         ^ 
   | 

1 error generated, 1 error printed.
*/

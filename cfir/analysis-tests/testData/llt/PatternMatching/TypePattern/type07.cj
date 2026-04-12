// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f

open class Father {
    public var age = 40
}

class Son <: Father {
    public var one: Int32 = 2
    public init() {
        age = 20
    }
}

main(): Int64 {
    var target = Father()
    var res = match (target) {
        case son: Son | father: Father => 0
        case _ => -1
    }
    if (res == 20) {
        return 0
    }
    return 1
}

/* SCAN
error: cannot introduce variables in patterns connected by '|'
  ==> type07.cj:23:14:
   | 
23 |         case son: Son | father: Father => 0
   |              ^^^        ~~~~~~ 
   | 

1 error generated, 1 error printed.
*/

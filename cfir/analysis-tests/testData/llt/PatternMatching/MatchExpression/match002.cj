// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func test(): Int64 {
    var i: Int32 = 0
    var res = match (i++) {
        case <!TYPE_MISMATCH!>0<!> => <!UNRESOLVED_REFERENCE!>res<!> = 1
        case <!TYPE_MISMATCH!>1<!> => <!UNRESOLVED_REFERENCE!>res<!> = 0
        case _ => <!UNRESOLVED_REFERENCE!>res<!> = -1
    }
    if (res == 0) {
        return 0
    }
    return 1
}

main(): Int64 {
    return test()
}

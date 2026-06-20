// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    if (<!ILLEGAL_ACCESS_NON_STATIC_MEMBER!>D<!>ata.n != 1) {
        return 1
    }
    <!ILLEGAL_ACCESS_NON_STATIC_MEMBER!>D<!>ata.m = 10
    if (<!ILLEGAL_ACCESS_NON_STATIC_MEMBER!>D<!>ata.m != 2) {
        return 2
    }
    return 0
}

enum Data {
    DAY
}

extend Data {
    public <!EXTEND_IMMUTABLE_MUT_PROPERTY!>mut<!> prop m: Int64 {
        get() {
            return 2
        }
        set(v) {}
    }
    public prop n: Int64 {
        get() {
            return 1
        }
    }
}

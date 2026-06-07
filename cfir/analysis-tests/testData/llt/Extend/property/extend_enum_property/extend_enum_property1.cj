// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    if (<!sema_illegal_access_non_static_member!>D<!>ata.n != 1) {
        return 1
    }
    <!sema_illegal_access_non_static_member!>D<!>ata.m = 10
    if (<!sema_illegal_access_non_static_member!>D<!>ata.m != 2) {
        return 2
    }
    return 0
}

enum Data {
    DAY
}

extend Data {
    public <!sema_immutable_type_illegal_property!>mut<!> prop m: Int64 {
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

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    return 0
}

abstract class Boo {
    private mut prop <!INVALID_MEMBER_VISIBILITY_IN_CLASS!>l<!>: Int32
    <!MISSING_FUNC_BODY!>p<!>ublic mut static prop m: Int32
}

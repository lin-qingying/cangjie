// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class C1 {

}

extend C1 {
    public func foo(): <!parse_this_type_not_allow!>This<!> {
        return C1()
    }
}

main() {
    return 1
}

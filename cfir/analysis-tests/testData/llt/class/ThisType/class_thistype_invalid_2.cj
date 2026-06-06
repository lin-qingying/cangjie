// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

class C1 {
    public func foo(): This {
        var a: <!parse_this_type_not_allow!>This<!> = this
        return this
    }
}

main() {
    return 1
}

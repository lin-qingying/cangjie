// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class Parent {
    protected var field: Int32 = 1
}

class Child <: Parent {
    public func main1() {
        var child: Child = Child()
        field = 2
        if (child.field != 1) {
            return 1
        }
        return 0
    }
}

<!sema_unexpected_param_for_entry!>m<!>ain(a: Int32): Int64 {
    return Child().main1()
}

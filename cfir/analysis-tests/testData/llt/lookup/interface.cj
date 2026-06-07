<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface C {
    static func <!sema_unable_to_infer_return_type!>foo<!>() {
        goo()
    }
    static func goo() {
        foo()
    }
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main() {
    let future: Future<Unit> = spawn {
        var future: Future<Unit> = 0
        return future
        <!parse_unexpected_declaration_in_scope!>main<!>() {}
    }
}

// Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main() {
    <!USE_EXPR_WITHOUT_IMPORT!>@IfAvailable(level:19, { => println(0) }, {=>println(3)})<!>
    <!USE_EXPR_WITHOUT_IMPORT!>@IfAvailable(syscap:"AA", { => println(0) }, {=>println(3)})<!>
}

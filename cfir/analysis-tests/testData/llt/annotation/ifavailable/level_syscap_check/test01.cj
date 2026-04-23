// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: canIUse.cj device_info.cj

main() {
    <!USE_EXPR_WITHOUT_IMPORT!>@IfAvailable(level: 19, {=>}, {=>})<!>
    <!USE_EXPR_WITHOUT_IMPORT!>@IfAvailable(syscap: "xxx", {=>}, {=>})<!>
}

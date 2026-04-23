// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main() {
    @IfAvailable(<!IFAVAILABLE_UNKNOWN_ARG_NAME!>Level<!>:11, {=>}, <!TYPE_MISMATCH!>{ a:Int=>() }<!>)
}

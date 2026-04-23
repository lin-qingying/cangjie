// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

public class TestThis {
    public func a(): This {
        this
    }
    public func c(): This <!TYPE_MISMATCH!>{
        a().a()
    }<!>
}

main() {}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

#include <stdio.h>

struct CStruct {
    int a[3];
    int b[0];
    int c[2];
    int d[4];
};

int f(struct CStruct s)
{
    if (s.a[0] == 1 && s.a[2] == 1 && s.c[0] == 2 && s.c[1] == 2 && s.d[0] == 3 && s.d[3] == 3) {
        return 0;
    }
    return 1;
}
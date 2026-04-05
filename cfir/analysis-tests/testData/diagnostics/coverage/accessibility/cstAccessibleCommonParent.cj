// RUN_PIPELINE_TILL: FRONTEND
// 测试 CST 可访问性过滤 — if 分支类型推断中过滤不可访问的公共父类型
// FILE: common.cj
package common

public interface Displayable {
    func display(): Unit {}
}

public class TextWidget <: Displayable {}
public class ImageWidget <: Displayable {}

// FILE: main.cj
package main

import common.*

// TextWidget 和 ImageWidget 都实现 Displayable（public + 已导入）
// CST([TextWidget, ImageWidget]) 的结果应包含 Displayable
func cstPublicParent(flag: Bool): Displayable {
    // if 分支推断时，公共父类型 Displayable 可访问
    let widget = if (flag) {
        TextWidget()
    } else {
        ImageWidget()
    }
    widget.display()
    return widget
}

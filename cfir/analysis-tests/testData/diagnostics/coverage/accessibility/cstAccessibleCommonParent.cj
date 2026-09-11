// RUN_PIPELINE_TILL: FRONTEND
// 公开共同接口不保证唯一可见 Join；共同公开类与显式接口目标均应保持可访问。
// FILE: common.cj
package common

public interface Displayable {
    func display(): Unit {}
}

public class TextWidget <: Displayable {}
public class ImageWidget <: Displayable {}

public open class Widget <: Displayable {}
public class TextWithParent <: Widget {}
public class ImageWithParent <: Widget {}

// FILE: main.cj
package localmain

import common.*

// 官方 cjc 对这两个无共同类的实现报告无最小公共父类型。
func cstPublicParent(flag: Bool): Displayable {
    let widget = <!TYPE_INCOMPATIBLE!>if (flag) {
        TextWidget()
    } else {
        ImageWidget()
    }<!>
    widget.display()
    return widget
}

func cstPublicClassParent(flag: Bool): Displayable {
    let widget = if (flag) { TextWithParent() } else { ImageWithParent() }
    widget.display()
    return widget
}

func explicitPublicInterface(flag: Bool): Displayable {
    let widget: Displayable = if (flag) { TextWidget() } else { ImageWidget() }
    widget.display()
    return widget
}

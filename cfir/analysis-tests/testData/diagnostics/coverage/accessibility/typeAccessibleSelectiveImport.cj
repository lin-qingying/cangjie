// RUN_PIPELINE_TILL: FRONTEND
// 测试类型可访问性 — 跨包选择性导入
// FILE: shapes.cj
package shapes

public interface Drawable {
    func draw(): Unit {}
}

public interface Resizable {
    func resize(): Unit {}
}

public class Circle <: Drawable {}
public class Square <: Drawable & Resizable {}

// FILE: main.cj
package localmain

import shapes.Circle
import shapes.Drawable

// 只导入了 Circle 和 Drawable，没有导入 Square 和 Resizable
func useSelectiveImport(): Unit {
    let c = Circle()
    c.draw()  // OK: Circle 和 Drawable 都已导入
}

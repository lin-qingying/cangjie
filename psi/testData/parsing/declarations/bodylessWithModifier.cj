// 无体成员 + 显式 `abstract` 修饰符：本解析器接受（PSI 零错误）。
// 官方对照：cjc 1.0.5 **拒绝**此形态（unexpected modifier 'abstract' on function declaration
// in class body），1.1.3 仅对 CJMP common/specific 抽象类放行。
// 这属**修饰符合法性**（parse_illegal_modifier_in_scope），与本文件的"缺体"判定是两条独立规则：
// 缺体由宿主作用域决定（class/interface 体隐式 abstract），修饰符合法性尚未在本仓实现。
// 保留本文件用于锁定"缺体不因修饰符而报错"的现状，不宣称该形态在官方合法。
// 官方接受的「无修饰符无体」形态见 abstractClass.cj 的 Animal1。
abstract class ModifierShape {
    abstract func area(): Float64
}

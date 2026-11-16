// RUN_PIPELINE_TILL: FRONTEND
// 裸名字 enum pattern 的「分类」与「定位」。
//
// 官方实现（external/cangjie_compiler/src/Sema/TypeCheckPattern.cpp）：
// - ChkVarOrEnumPattern：名字只要是可见 enum constructor（ASTContext::IsEnumConstructor，与 subject
//   类型、同名局部变量或形参、候选是否唯一都无关）就是 enum pattern，否则才是绑定变量；
// - FindEnumPatternTarget：构造器目标只在 subject enum 声明内查找（FieldLookup），找不到报
//   sema_undeclared_identifier，只有 payload 个数不匹配时报 sema_enum_pattern_param_size_error；
// - ChkEnumPattern：subject 不是 enum 时报 sema_pattern_not_match。
//
// 每个函数都与文件头的类型声明、helper 函数（weigh / applyToShape / each）一起用 cjc 1.0.5 / 1.1.3
// 单独编译取证，两版结论一致；标记为本项目诊断名：
// sema_undeclared_identifier -> UNRESOLVED_REFERENCE、sema_pattern_not_match -> PATTERN_NOT_MATCH、
// sema_enum_pattern_param_size_error -> ENUM_PATTERN_PARAM_SIZE_ERROR。
//
// 本文件只放 sema 阶段的诊断：文件里有 sema 错误时官方不进入 CHIR，`chir_unreachable_pattern` 之类的
// 结论要放在无错误的夹具里（见 matchPatternKnownGenericConstructor.cj）。

enum Shape {
    | Circle
    | Square
}

enum Color {
    | Red
    | Green
    | Blue(Int64)
}

enum Token {
    | Circle
    | Dot
}

enum Paper {
    | Blank
    | Lined
}

enum Ink {
    | Blank
    | Dark
}

enum Fit {
    | Loose
    | Loose(Int64)
}

enum Gauge {
    | Level(Int64)
    | Off
}

enum Rank {
    | Level
    | Top
}

enum Box {
    | Wrap(Shape)
    | Empty
}

enum Solo {
    | Only(Shape)
}

enum Slot<T> {
    | Free
    | Used(T)
}

class Holder {}

func weigh(shape: Shape): Int64 {
    match (shape) {
        case Circle => 1
        case Square => 2
    }
}

func applyToShape(f: (Shape) -> Int64): Int64 {
    f(Square)
}

func each<T>(items: Array<T>, f: (T) -> Int64): Int64 {
    var total = 0
    for (item in items) {
        total = total + f(item)
    }
    total
}

// ── subject 是 enum：名字是别的 enum 的 constructor，必须报未声明，不能绑定到那个 enum ──

func foreignBare(shape: Shape): Int64 {
    match (shape) {
        case <!UNRESOLVED_REFERENCE!>Green<!> => 1
        case _ => 2
    }
}

func foreignBareWithPayloadConstructor(shape: Shape): Int64 {
    match (shape) {
        case <!UNRESOLVED_REFERENCE!>Blue<!> => 1
        case _ => 2
    }
}

func foreignWithArguments(shape: Shape): Int64 {
    match (shape) {
        case <!UNRESOLVED_REFERENCE!>Blue<!>(_) => 1
        case _ => 2
    }
}

func foreignNestedInPayload(box: Box): Int64 {
    match (box) {
        case Wrap(<!UNRESOLVED_REFERENCE!>Green<!>) => 1
        case _ => 2
    }
}

func foreignNestedInOption(value: ?Shape): Int64 {
    match (value) {
        case Some(<!UNRESOLVED_REFERENCE!>Green<!>) => 1
        case _ => 2
    }
}

func foreignInTuple(shape: Shape, count: Int64): Int64 {
    match ((shape, count)) {
        case (<!UNRESOLVED_REFERENCE!>Green<!>, 1) => 1
        case _ => 2
    }
}

func foreignInOrPattern(shape: Shape): Int64 {
    match (shape) {
        case Circle | <!UNRESOLVED_REFERENCE!>Green<!> => 1
        case _ => 2
    }
}

func foreignInIfLet(shape: Shape): Int64 {
    if (let <!UNRESOLVED_REFERENCE!>Green<!> <- shape) {
        return 1
    }
    return 2
}

func foreignInWhileLet(shape: Shape): Int64 {
    var count = 0
    while (let <!UNRESOLVED_REFERENCE!>Green<!> <- shape) {
        count = count + 1
        break
    }
    return count
}

func foreignNestedInLetCondition(box: Box): Int64 {
    if (let Wrap(<!UNRESOLVED_REFERENCE!>Green<!>) <- box) {
        return 1
    }
    return 2
}

func foreignOnOptionSubject(value: ?Int64): Int64 {
    match (value) {
        case <!UNRESOLVED_REFERENCE!>Green<!> => 1
        case _ => 2
    }
}

func stdlibConstructorOnUserEnum(shape: Shape): Int64 {
    match (shape) {
        case <!UNRESOLVED_REFERENCE!>None<!> => 1
        case _ => 2
    }
}

func foreignOnGenericEnumSubject(slot: Slot<Int64>): Int64 {
    match (slot) {
        case <!UNRESOLVED_REFERENCE!>Green<!> => 1
        case _ => 2
    }
}

// ── subject 是 lambda 形参：形参类型来自期望函数类型、函数形参类型或泛型实参推断 ──

func foreignInLambdaWithExpectedFunctionType(): Int64 {
    let pick: (Shape) -> Int64 = { shape =>
        match (shape) {
            case <!UNRESOLVED_REFERENCE!>Green<!> => 1
            case _ => 0
        }
    }
    pick(Square)
}

func foreignInLambdaArgument(): Int64 {
    applyToShape { shape =>
        match (shape) {
            case <!UNRESOLVED_REFERENCE!>Green<!> => 1
            case _ => 0
        }
    }
}

func foreignInLambdaArgumentOfGenericFunction(shapes: Array<Shape>): Int64 {
    each(shapes) { shape =>
        match (shape) {
            case <!UNRESOLVED_REFERENCE!>Green<!> => 1
            case _ => 0
        }
    }
}

// ── 分类只看「是否存在同名 enum constructor」：候选不唯一、被局部名字遮蔽都不改变结论 ──

func foreignDeclaredByTwoEnums(shape: Shape): Int64 {
    match (shape) {
        case <!UNRESOLVED_REFERENCE!>Blank<!> => 1
        case _ => 2
    }
}

func foreignOverloadedByArity(shape: Shape): Int64 {
    match (shape) {
        case <!UNRESOLVED_REFERENCE!>Loose<!> => 1
        case _ => 2
    }
}

func shadowedByLocalVariable(shape: Shape): Int64 {
    let Green = 7
    match (shape) {
        case <!UNRESOLVED_REFERENCE!>Green<!> => 1
        case _ => Green
    }
}

func shadowedByParameter(shape: Shape, Green: Int64): Int64 {
    match (shape) {
        case <!UNRESOLVED_REFERENCE!>Green<!> => 1
        case _ => Green
    }
}

// ── subject enum 内有同名 constructor 但 payload 个数不匹配：定位到它并报个数错误 ──

func ownPayloadConstructorMatchedBare(gauge: Gauge): Int64 {
    match (gauge) {
        case <!ENUM_PATTERN_PARAM_SIZE_ERROR!>Level<!> => 1
        case _ => 2
    }
}

// ── subject 不是 enum：仍是 enum pattern，报 pattern 与 subject 不匹配 ──

func foreignOnIntegerSubject(count: Int64): Int64 {
    match (count) {
        case <!PATTERN_NOT_MATCH!>Green<!> => 1
        case _ => 2
    }
}

func foreignOnClassSubject(holder: Holder): Int64 {
    match (holder) {
        case <!PATTERN_NOT_MATCH!>Green<!> => 1
        case _ => 2
    }
}

func foreignOnTypeParameterSubject<T>(value: T): Int64 {
    match (value) {
        case <!PATTERN_NOT_MATCH!>Green<!> => 1
        case _ => 2
    }
}

// ── 正例：同名 constructor 按 subject enum 定位，零诊断 ──

func ownConstructorSharedWithOtherEnum(shape: Shape, token: Token): Int64 {
    let left = match (shape) {
        case Circle => 1
        case Square => 2
    }
    let right = match (token) {
        case Circle => 10
        case Dot => 20
    }
    left + right
}

func ownConstructorsByArity(gauge: Gauge, rank: Rank): Int64 {
    let left = match (gauge) {
        case Level(value) => value
        case Off => 0
    }
    let right = match (rank) {
        case Level => 1
        case Top => 2
    }
    left + right
}

func ownConstructorInNestedPositions(box: Box, value: ?Shape): Int64 {
    let left = match (box) {
        case Wrap(Circle) => 1
        case Wrap(Square) => 2
        case Empty => 3
    }
    let right = match (value) {
        case Some(Circle) => 10
        case Some(Square) => 20
        case None => 30
    }
    left + right
}

func ownConstructorInTupleAndOrPattern(shape: Shape, token: Token): Int64 {
    let left = match ((shape, token)) {
        case (Circle, Circle) => 1
        case (Circle, Dot) => 2
        case (Square, _) => 3
    }
    let right = match (shape) {
        case Circle | Square => 10
    }
    left + right
}

func ownConstructorInLetCondition(token: Token): Int64 {
    if (let Dot <- token) {
        return 1
    }
    return 2
}

func ownConstructorOfGenericEnum(slot: Slot<Int64>): Int64 {
    match (slot) {
        case Free => 0
        case Used(value) => value
    }
}

func ownConstructorInLambdaBody(shapes: Array<Shape>): Int64 {
    each(shapes) { shape =>
        match (shape) {
            case Circle => 1
            case Square => 2
        }
    }
}

// ── 正例：声明位置的裸名字永远是绑定变量（官方 ParsePattern 的 inDecl），与同名 constructor 无关 ──

func declarationPatternsAlwaysBind(shapes: Array<Shape>, shape: Shape): Int64 {
    var total = 0
    for (Green in shapes) {
        total = total + weigh(Green)
    }
    let Blank = shape
    let (Loose, Level) = (shape, 1)
    total + weigh(Blank) + weigh(Loose) + Level
}

func forInNestedBareNamesBind(pairs: Array<(Shape, Int64)>, solos: Array<Solo>): Int64 {
    var total = 0
    for ((Green, Red) in pairs) {
        total = total + weigh(Green) + Red
    }
    for (Only(Green) in solos) {
        total = total + weigh(Green)
    }
    total
}

func forInAmbiguousAndOwnNamesBind(shapes: Array<Shape>): Int64 {
    var total = 0
    for (Loose in shapes) {
        total = total + weigh(Loose)
    }
    for (Blank in shapes) {
        total = total + weigh(Blank)
    }
    for (Circle in shapes) {
        total = total + weigh(Circle)
    }
    total
}

func forInGuardSeesBinding(shapes: Array<Shape>): Int64 {
    var total = 0
    for (Green in shapes where weigh(Green) > 1) {
        total = total + weigh(Green)
    }
    total
}

main(): Int64 {
    return 0
}

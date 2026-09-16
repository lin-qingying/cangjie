package org.cangnova.cangjie

/**
 * 能自证源文件种类的对象。
 *
 * 由 `psi` 模块的 PSI 文件实现；`common` 只声明契约，不反向依赖 `psi`。
 * 依赖方向的必然结果：`CjPsiSourceFile` 包装 `PsiFile`，而权威的"是否为 `.cj.d`"答案在 PSI 侧，
 * 但 `CjFile` 定义在 `psi` 模块（依赖方向为 `psi -> common`），故此处以一个可实现的契约做依赖倒置。
 */
interface CjSourceKindCarrier {
    /** 该对象所承载文件的种类。 */
    val sourceKind: CjSourceKind
}

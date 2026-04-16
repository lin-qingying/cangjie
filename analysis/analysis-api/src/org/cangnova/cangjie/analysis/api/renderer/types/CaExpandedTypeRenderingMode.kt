package org.cangnova.cangjie.analysis.api.renderer.types

public enum class CaExpandedTypeRenderingMode {
    /**
     * Renders only the abbreviated type as-is, e.g. `foo.bar.StringAlias`.
     */
    RENDER_ABBREVIATED_TYPE,

    /**
     * Renders the abbreviated type as-is and its expansion in a comment, e.g. `foo.bar.StringAlias /* = kotlin.String */`.
     */
    RENDER_ABBREVIATED_TYPE_WITH_EXPANDED_TYPE_COMMENT,

    /**
     * Renders the expanded type as-is, e.g. `kotlin.String`.
     */
    RENDER_EXPANDED_TYPE,

    /**
     * Renders the expanded type as-is and its abbreviated type in a comment, e.g. `kotlin.String /* from: foo.bar.StringAlias */`.
     */
    RENDER_EXPANDED_TYPE_WITH_ABBREVIATED_TYPE_COMMENT,
}

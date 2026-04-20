package org.cangnova.cangjie.analysis.api.evaluation

interface CaCollectionCompileTimeValue : CaCompileTimeValue {
    val elements: List<CaCompileTimeValue>
}

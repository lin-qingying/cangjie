package org.cangnova.cangjie.analysis.api.evaluation

interface CaTupleCompileTimeValue : CaCompileTimeValue {
    val elements: List<CaCompileTimeValue>
}

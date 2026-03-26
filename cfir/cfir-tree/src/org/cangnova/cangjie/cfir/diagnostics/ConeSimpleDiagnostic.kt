package org.cangnova.cangjie.cfir.diagnostics

import org.cangnova.cangjie.cfir.types.ConeDiagnostic

class ConeSimpleDiagnostic(override val reason: String, val kind: DiagnosticKind = DiagnosticKind.Other) :
    ConeDiagnostic
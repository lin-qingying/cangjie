// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

/**
 * @file
 *
 * This file implements annotation matching and propagation logic for the CJMP (Cangjie Multi-Specific) feature.
 * It ensures that annotations between common declarations and specific-specific declarations are properly synchronized.
 *
 * Key Components:
 * - MatchCJMPDeclAnnotations(): Validates that common and specific declarations have matching annotations
 * - PropagateCJMPDeclAnnotations(): Propagates special annotations from common to specific declarations
 * - CheckNotAllowedAnnotations(): Validates that unsupported annotations are not used on common/specific declarations
 *
 * The implementation maintains consistency between common and specific-specific code annotations in the
 * multi-specific compilation model, ensuring proper annotation inheritance and synchronization.
 */
#include "MPTypeCheckerImpl.h"
#include "TypeCheckUtil.h"
#include "cangjie/AST/AttributePack.h"
#include "cangjie/AST/Clone.h"
#include "cangjie/AST/Node.h"
#include "cangjie/AST/Types.h"
#include "cangjie/AST/Walker.h"
#include "cangjie/Basic/DiagnosticEngine.h"
#include "cangjie/Basic/Position.h"
#include "cangjie/Parse/Parser.h"
#include "cangjie/Utils/CastingTemplate.h"
#include "cangjie/Utils/CheckUtils.h"
#include "cangjie/Utils/SafePointer.h"
#include <algorithm>
#include <unordered_set>

using namespace Cangjie;
using namespace AST;
using namespace TypeCheckUtil;

namespace {
// Annotations not serialized in AST are matched via their corresponding attributes
// Annotations with parameters MUST be serialized for common/specific matching
const std::unordered_map<AnnotationKind, Attribute> NON_SERIALIZED_ANNOTATIONS = {
    {AnnotationKind::C, Attribute::C},
    {AnnotationKind::JAVA_MIRROR, Attribute::JAVA_MIRROR},
    {AnnotationKind::JAVA_HAS_DEFAULT, Attribute::JAVA_HAS_DEFAULT},
    {AnnotationKind::OBJ_C_MIRROR, Attribute::OBJ_C_MIRROR},
};

// Annotations not supported on common/specific (neither serialized nor have attributes)
const std::unordered_set<AnnotationKind> UNSUPPORTED_ANNOTATIONS = {AnnotationKind::JAVA, AnnotationKind::CALLING_CONV,
    AnnotationKind::CONSTSAFE, AnnotationKind::ENSURE_PREPARED_TO_MOCK, AnnotationKind::UNKNOWN};

bool PostCheckDeprecatedAnnotation(const AST::Decl& specific, DiagnosticEngine& diag)
{
    auto specificDeprecation = FindFirstAnnotation(specific, AnnotationKind::DEPRECATED);
    if (specificDeprecation && !specificDeprecation->TestAttr(Attribute::COMPILER_ADD)) {
        diag.DiagnoseRefactor(DiagKindRefactor::sema_specific_has_deprecated_annotation, *specificDeprecation,
                "Deprecated", DeclKindToString(specific), specific.identifier.Val()).
                AddNote("specific declarations implicitly inherit deprecation from the common declarations");
        return false;
    }

    return true;
}

bool FuncArgEquals(const AST::FuncArg& a, const AST::FuncArg& b)
{
    if (a.name != b.name) {
        return false;
    }

    if (a.expr->astKind != ASTKind::LIT_CONST_EXPR || b.expr->astKind != ASTKind::LIT_CONST_EXPR) {
        // const expressions passing to annotations are not supported
        return false;
    }

    auto aLit = StaticAs<ASTKind::LIT_CONST_EXPR>(a.expr.get());
    auto bLit = StaticAs<ASTKind::LIT_CONST_EXPR>(b.expr.get());

    if (aLit->kind != bLit->kind) {
        return false;
    }

    return aLit->ToString() == bLit->ToString();
}

bool AnnotationEquals(const AST::Annotation& a, const AST::Annotation& b)
{
    if (a.kind != b.kind) {
        return false;
    }
    if (a.kind == AnnotationKind::CUSTOM && a.identifier.Val() != b.identifier.Val()) {
        return false;
    }
    if (a.args.size() != b.args.size()) {
        return false;
    }

    // named parameters are not supported but we check parameter names just in case
    // inside of FuncArgEquals, will only work if named arguments are at the same positions
    for (size_t i = 0; i < a.args.size(); ++i) {
        if (!FuncArgEquals(*a.args[i], *b.args[i])) {
            return false;
        }
    }

    return true;
}

Ptr<const AST::Annotation> FindAnnotation(const AST::Decl& decl, const AST::Annotation& sample,
    const std::unordered_set<Ptr<const AST::Annotation>>& excludes)
{
    auto found = std::find_if(decl.annotations.begin(), decl.annotations.end(), [&sample, &excludes](const auto& anno) {
        return excludes.count(anno.get()) == 0 && AnnotationEquals(sample, *anno.get());
    });
    if (found != decl.annotations.end()) {
        return (*found).get();
    }
    return nullptr;
}

std::string FormatAnnotationMismatchNote(const AST::Annotation& anno)
{
    std::ostringstream result;
    result << "The mismatched annotation is @" << anno.identifier.Val();
    if (!anno.args.empty()) {
        result << "(";
        int index = 0;
        for (auto& arg : anno.args) {
            if (index > 0) {
                result << ", ";
            }
            index++;
            result << arg->ToString();
        }
        result << ")";
    }
    return result.str();
}

std::string FormatAnnotationMismatchNote(AnnotationKind kind)
{
    std::ostringstream noteMessage;
    noteMessage << "The mismatched annotation is @" << AnnotationKindToString(kind);
    return noteMessage.str();
}

void ReportAnnotationMismatch(const AST::Annotation& anno, const AST::Decl& decl, DiagnosticEngine& diag)
{
    if (decl.TestAttr(Attribute::SPECIFIC)) {
        // The declaration is specific-specific but the mismatching annotation belongs to the common declaration.
        // We report the error at the specific declaration location since it has source positions.
        // The common annotation is deserialized from metadata and lacks source position information,
        // so we cannot provide a hint pointing to it. Instead, we generate a descriptive note
        // explaining which annotation is mismatched.
        auto note = FormatAnnotationMismatchNote(anno);
        // note: decl is specific decl
        diag.DiagnoseRefactor(DiagKindRefactor::sema_specific_has_different_annotation, decl, DeclKindToString(decl))
            .AddNote(note);
    } else {
        // note: decl is common here
        auto msg = diag.DiagnoseRefactor(
            DiagKindRefactor::sema_specific_has_different_annotation, anno, DeclKindToString(decl));
        msg.AddHint(decl, "The corresponding common declaration");
        msg.AddNote("Either remove the specific annotation or add it to the common declaration");
    }
}

bool PostCheckNonSerializedAnnotations(const AST::Decl& common, const AST::Decl& specific, DiagnosticEngine& diag)
{
    bool result = true;

    // Iterate over all non-serialized annotations and check attribute consistency
    for (const auto& [annoKind, attr] : NON_SERIALIZED_ANNOTATIONS) {
        if (common.TestAttr(attr) != specific.TestAttr(attr)) {
            // Find the specific annotation for this type to use in diagnostic
            auto specificAnno = FindFirstAnnotation(specific, annoKind);
            if (specificAnno) {
                // we have a specific annotation but common doesn't have it
                ReportAnnotationMismatch(*specificAnno, specific, diag);
            } else {
                // there is an annotation in common but specific doesn't have it
                // for non-serialized annotations from common we don't have an instance
                // so we cannot use ReportAnnotationMismatch function
                diag.DiagnoseRefactor(
                    DiagKindRefactor::sema_specific_has_different_annotation, specific, DeclKindToString(specific)).
                    AddNote(FormatAnnotationMismatchNote(annoKind));
            }
            result = false;
        }
    }

    return result;
}

bool IsSpecialHandledAnnotation(const Annotation& anno)
{
    return anno.kind == AnnotationKind::DEPRECATED || anno.kind == AnnotationKind::ATTRIBUTE ||
        NON_SERIALIZED_ANNOTATIONS.find(anno.kind) != NON_SERIALIZED_ANNOTATIONS.end() ||
        UNSUPPORTED_ANNOTATIONS.find(anno.kind) != UNSUPPORTED_ANNOTATIONS.end();
}

/**
 * @brief Matches annotations from source declaration to target declaration.
 *
 * This helper function performs the core annotation matching logic for one direction
 * (either common→specific or specific→common). It iterates through all annotations
 * on the source declaration and attempts to find matching annotations on the target
 * declaration, reporting mismatches when found.
 *
 * @param source The declaration containing annotations to match
 * @param target The declaration to find matching annotations in
 * @param matchedSource Set to track annotations already matched on source side
 * @param matchedTarget Set to track annotations already matched on target side
 * @param diag Diagnostic engine for reporting mismatches
 * @return bool True if all source annotations matched successfully, false otherwise
 */
bool MatchAnnotationsFromSource(const AST::Decl& source, const AST::Decl& target,
    std::unordered_set<Ptr<const AST::Annotation>>& matchedSource,
    std::unordered_set<Ptr<const AST::Annotation>>& matchedTarget, DiagnosticEngine& diag)
{
    bool result = true;

    for (auto& anno : source.annotations) {
        if (IsSpecialHandledAnnotation(*anno) || matchedSource.count(anno.get())) {
            continue;
        }
        auto targetMatch = FindAnnotation(target, *anno.get(), matchedTarget);
        if (targetMatch) {
            matchedSource.insert(anno.get());
            matchedTarget.insert(targetMatch);
        } else {
            ReportAnnotationMismatch(*anno.get(), target, diag);
            result = false;
        }
    }

    return result;
}
} // namespace

/**
 * @brief Validates that common and specific declarations have matching annotations.
 *
 * This function implements the CJMP rule that common and specific declarations must have
 * matching annotations (except for @Deprecated which is handled separately). It performs
 * a bidirectional check to ensure all annotations on both declarations have corresponding
 * matches on the other side.
 *
 * The algorithm:
 * 1. Checks deprecation annotation rules first via PostCheckDeprecatedAnnotation
 * 2. For each annotation on the common declaration, finds a matching annotation on the specific declaration
 * 3. For each annotation on the specific declaration, finds a matching annotation on the common declaration
 * 4. Reports mismatches with detailed diagnostic messages
 *
 * @note This function handles the case when the same annotation is applied multiple times
 *       to the same declaration. Each occurrence is matched separately to ensure the exact
 *       count of annotations matches between common and specific declarations.
 * @note The function matches annotation arguments (position-based) but doesn't handle named parameters.
 *       Only positional arguments are supported for annotation matching.
 *
 * @param common The common declaration to match annotations from
 * @param specific The specific declaration to match annotations against
 * @return bool True if all annotations match successfully, false otherwise
 */
bool MPTypeCheckerImpl::MatchCJMPDeclAnnotations(const AST::Decl& common, AST::Decl& specific) const
{
    bool result = PostCheckDeprecatedAnnotation(specific, diag);
    result &= PostCheckNonSerializedAnnotations(common, specific, diag);

    // Two sets are required to track which annotations have already been matched to handle
    // duplicate annotations correctly. They ensure each annotation on one side finds exactly
    // one matching annotation on the other side, preventing multiple annotations from
    // matching the same target.
    std::unordered_set<Ptr<const AST::Annotation>> matchedCommonAnnotations;
    std::unordered_set<Ptr<const AST::Annotation>> matchedSpecificAnnotations;

    // Match annotations from common declaration to specific declaration
    result &= MatchAnnotationsFromSource(common, specific, matchedCommonAnnotations, matchedSpecificAnnotations, diag);

    // Match annotations from specific declaration to common declaration
    result &= MatchAnnotationsFromSource(specific, common, matchedSpecificAnnotations, matchedCommonAnnotations, diag);

    return result;
}

namespace {
bool IsCommonOrSpecific(const Node& node)
{
    return node.TestAnyAttr(Attribute::COMMON, Attribute::SPECIFIC);
}

bool IsUnsupported(const Annotation& anno)
{
    return UNSUPPORTED_ANNOTATIONS.count(anno.kind);
}
} // namespace

void MPTypeCheckerImpl::CheckNotAllowedAnnotations(AST::Package& pkg)
{
    std::function<VisitAction(Ptr<Node>)> visitor = [this](const Ptr<Node>& node) {
        if (node->IsDecl() && IsCommonOrSpecific(*node) && !node->TestAttr(Attribute::FROM_COMMON_PART)) {
            for (auto& anno : StaticCast<Decl>(node)->annotations) {
                if (IsUnsupported(*anno)) {
                    diag.DiagnoseRefactor(DiagKindRefactor::sema_common_specific_annotation_not_allowed, *anno,
                            AnnotationKindToString(anno->kind))
                        .AddNote("This annotation kind is not supported on common/specific");
                }
            }
        }
        if (node->astKind == ASTKind::FUNC_BODY) {
            return VisitAction::SKIP_CHILDREN;
        }
        return VisitAction::WALK_CHILDREN;
    };
    Walker walker(&pkg, visitor);
    walker.Walk();
}

namespace {
/**
 * @brief Propagates deprecation annotations from common declarations to specific declarations.
 *
 * This function implements the CJMP rule that specific declarations implicitly inherit
 * deprecation annotations from their corresponding common declarations. It only copies
 * the @Deprecated annotation if the common declaration has one and the specific declaration
 * doesn't already have one.
 *
 * @param common The common declaration that may contain deprecation annotation
 * @param specific The specific declaration to receive the deprecation annotation (if applicable)
 */
void PropagateDeprecatedAnnotations(const AST::Decl& common, AST::Decl& specific)
{
    auto commonDeprecation = FindFirstAnnotation(common, AnnotationKind::DEPRECATED);
    auto specificDeprecation = FindFirstAnnotation(specific, AnnotationKind::DEPRECATED);

    if (!commonDeprecation || specificDeprecation) {
        return;
    }

    specific.annotations.emplace_back(ASTCloner::Clone(commonDeprecation));
}

/**
 * @brief Propagates @Attribute annotations from common declarations to specific declarations.
 *
 * @Attribute annotations are handled via propagation rather than matching since they
 * don't have corresponding attributes for comparison. This function copies all @Attribute
 * annotations from the common declaration to the specific declaration.
 *
 * @note Only @Attribute annotations with "TEST_REGISTER" are serialized for now.
 *       The other @Attribute annotations are not serialized and will be silently ignored.
 *       In order to make it work, the proper serialization support needs to be implemented.
 *
 * @param common The common declaration containing @Attribute annotations
 * @param specific The specific declaration to receive the @Attribute annotations
 */
void PropagateAttributeAnnotations(const AST::Decl& common, AST::Decl& specific)
{
    for (auto& annotation : common.annotations) {
        if (annotation->kind == AnnotationKind::ATTRIBUTE) {
            specific.annotations.emplace_back(ASTCloner::Clone(annotation.get()));
        }
    }
}
} // namespace

/**
 * @brief Propagates special annotations from common declarations to specific declarations.
 *
 * This function handles annotations that cannot be matched directly between common and specific
 * declarations and must be propagated instead. Currently handles @Deprecated and @Attribute
 * annotations which have special propagation rules in CJMP.
 *
 * @param common The common declaration containing annotations to propagate
 * @param specific The specific declaration to receive the propagated annotations
 */
void MPTypeCheckerImpl::PropagateCJMPDeclAnnotations(const AST::Decl& common, AST::Decl& specific) const
{
    PropagateDeprecatedAnnotations(common, specific);
    PropagateAttributeAnnotations(common, specific);
}

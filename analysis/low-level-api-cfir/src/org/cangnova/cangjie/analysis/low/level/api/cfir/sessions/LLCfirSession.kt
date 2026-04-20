

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.ModificationTracker
import org.cangnova.cangjie.analysis.api.platform.lifetime.ModificationTrackerWithInvalidationReason
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirBuiltinTypes
import org.cangnova.cangjie.cfir.PrivateSessionConstructor
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicLongFieldUpdater
import kotlin.time.TimeSource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * An [LLCfirSession] stores all symbols, components, and configuration needed for the resolution of Kotlin code/binaries from a [CaModule].
 *
 * ### Invalidation
 *
 * [LLCfirSession] will be invalidated by [LLCfirSessionInvalidationService] when its [CaModule] or one of the module's dependencies is
 * modified, or when a global modification event occurs. Sessions are managed by [LLCfirSessionCache], which holds a soft reference to its
 * [LLCfirSession]s. This allows a session to be garbage collected when it is softly reachable. The session's [LLCfirSessionCleaner] ensures
 * that its associated [Disposable] is properly disposed even after garbage collection.
 *
 * When a session is invalidated after a modification event, the [LLCfirSessionInvalidationEventPublisher] will publish a
 * [session invalidation event][LLCfirSessionInvalidationTopics]. This allows entities whose lifetime depends on the session's lifetime to be
 * invalidated with the session. Such an event is not published when the session is garbage collected due to being softly reachable, because
 * the [LLCfirSessionCleaner] is not guaranteed to be executed in a write action. If we try to publish a session invalidation event outside
 * a write action, another thread might already have built another [LLCfirSession] for the same [CaModule], causing a race between the new
 * session and the session invalidation event (which can only refer to the [CaModule] because the session has already been garbage
 * collected).
 *
 * Because of this, it's important that cached entities which depend on a session's lifetime (and therefore its session invalidation events)
 * are *exactly as softly reachable* as the [LLCfirSession]. This means that the cached entity should keep a strong reference to the session,
 * but the entity itself should be softly reachable if not currently in use. For example, `CaCfirSession`s are softly reachable via
 * `CaCfirSessionProvider`, but keep a strong reference to the [LLCfirSession].
 */
@OptIn(PrivateSessionConstructor::class)
abstract class LLCfirSession(
    val caModule: CaModule,
    builtinTypes: CfirBuiltinTypes,
    kind: Kind
) : CfirSession(kind) {
    init {
        register(CfirBuiltinTypes::class, builtinTypes)
    }

    abstract fun getScopeSession(): ScopeSession

    val project: Project
        get() = caModule.project

    /**
     * The session's UUID to identify it for diagnostic purposes.
     *
     * For example, the UUID can be used to identify a session across multiple session structure files (see
     * [LLSessionStructureWriter][org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.structure.LLSessionStructureWriter]).
     */
    @OptIn(ExperimentalUuidApi::class)
    val uuid: Uuid = Uuid.random()

    /**
     * Whether the [LLCfirSession] is valid. The session should not be used if it is invalid.
     */
    @Volatile
    var isValid: Boolean = true
        internal set

    /**
     * Information about where the invalidation occurred used for diagnostic purposes.
     */
    var invalidationInformation: String? = null
        internal set

    /**
     * The time at which the session was created. This is used for diagnostic purposes.
     */
    val creationTimeMark: TimeSource.Monotonic.ValueTimeMark = TimeSource.Monotonic.markNow()

    /**
     * Creates a [ModificationTracker] which tracks the validity of this session via [isValid].
     */
    fun createValidityTracker(): ModificationTracker = LLCfirSessionValidityModificationTracker(WeakReference(this))

    private val lazyDisposable: Lazy<Disposable> = lazy {
        val disposable = Disposer.newDisposable()

        // `LLCfirSessionCache` is used as a disposable parent so that disposal is triggered after the Kotlin plugin is unloaded. We don't
        // register a module as a disposable parent, because (1) IJ `Module`s may persist beyond the plugin's lifetime, (2) not all
        // `CaModule`s have a corresponding `Module`, and (3) sessions are invalidated (and subsequently cleaned up) when their module is
        // removed.
        Disposer.register(LLCfirSessionCache.getInstance(project), disposable)

        disposable
    }

    /**
     * Returns an already registered [Disposable] which is alive until the session is invalidated. It can be used as a parent disposable for
     * disposable session components, such as [resolve extensions][org.cangnova.cangjie.analysis.api.resolve.extensions.CaResolveExtension].
     * When the session is invalidated or garbage-collected, all disposable session components will be disposed with this parent disposable.
     *
     * Because not all sessions have disposable components, this disposable is created and registered on-demand with the first call to
     * [requestDisposable]. This avoids polluting [Disposer] with unneeded disposables.
     *
     * The disposable must only be requested during session creation, before the session is added to [LLCfirSessionCache].
     */
    fun requestDisposable(): Disposable = lazyDisposable.value

    /**
     * A [Disposable] that has already been requested with [requestDisposable], or `null` otherwise.
     */
    internal val requestedDisposableOrNull: Disposable?
        get() = if (lazyDisposable.isInitialized()) lazyDisposable.value else null

    override fun toString(): String {
        return "${this::class.simpleName} for ${caModule.moduleDescription}"
    }
}

/**
 * The validity tracker must not strongly reference the session to avoid leaking it, as the validity tracker may survive it.
 *
 * Similarly, by convention, the [LLCfirSession] doesn't keep a strong reference to the validity tracker, to avoid overly complicated
 * reference cycles (from the developer's perspective).
 */
private class LLCfirSessionValidityModificationTracker(private val sessionRef: WeakReference<LLCfirSession>) : ModificationTrackerWithInvalidationReason {
    @Suppress("Unused")
    @Volatile
    private var count = 0L

    override fun getModificationCount(): Long {
        if (sessionRef.get()?.isValid == true) return 0

        // When the session is invalid, we cannot simply return a static modification count of 1. For example, consider situations where
        // a cached value was created with an already invalid session (so it remembers the modification count of 1). Then, if we return
        // a static modification count of 1, the modification count never changes and the cached value misses that the session has been
        // invalidated. Hence, `count` is incremented on each modification count access.
        return COUNT_UPDATER.incrementAndGet(this)
    }

    override fun getInvalidationReason(): String? {
        val session = sessionRef.get()
            ?: return "`${LLCfirSession::class.simpleName}` is garbage collected"
        if (!session.isValid) {
            return "`${session::class.simpleName}` for `${session.caModule::class.simpleName}` is invalid"
        }
        return null
    }

    companion object {
        private val COUNT_UPDATER = AtomicLongFieldUpdater.newUpdater(LLCfirSessionValidityModificationTracker::class.java, "count")
    }
}

abstract class LLCfirModuleSession(
    caModule: CaModule,
    builtinTypes: CfirBuiltinTypes,
    kind: Kind
) : LLCfirSession(caModule, builtinTypes, kind)

val CfirElementWithResolveState.llCfirSession: LLCfirSession
    get() = moduleData.session as LLCfirSession

val CfirBasedSymbol<*>.llCfirSession: LLCfirSession
    get() = cfir.moduleData.session as LLCfirSession

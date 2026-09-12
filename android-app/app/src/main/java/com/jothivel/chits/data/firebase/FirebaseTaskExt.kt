package com.jothivel.chits.data.firebase

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Minimal `Task.await()` so Firestore calls can be used from coroutines without pulling in
 * the kotlinx-coroutines-play-services artifact just for this one extension.
 */
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener {
        if (cont.isActive) cont.resumeWithException(kotlinx.coroutines.CancellationException("Task was cancelled"))
    }
}

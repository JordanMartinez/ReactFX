package org.reactfx.collection;

import org.reactfx.Guard;
import org.reactfx.Suspendable;

/**
 * An observable list whose items are the same as its {@code sourceList} when unsuspended; once suspended,
 * any changes to the {@code sourceList} will not propagate to this list until this list is unsuspended.
 */
public interface SuspendableList<E>
extends LiveList<E>, Suspendable {
}
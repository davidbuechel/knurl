package de.knurl.domain.calc

import de.knurl.domain.model.Exercise

/** Resolves an exercise ID: built-in catalog first, then the user's own exercises. */
typealias ExerciseLookup = (String) -> Exercise?

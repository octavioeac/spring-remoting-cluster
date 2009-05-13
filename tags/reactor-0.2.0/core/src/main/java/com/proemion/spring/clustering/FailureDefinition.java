package com.proemion.spring.clustering;

import java.net.URI;

/**
 * A Definition to be filled by a {@link FailureHandler}
 * A {@link FailureHandler} can implement this interface and store implementation specific failure information for a single {@link RemoteService} in it.
 * @author Steve Ulrich
 *
 */
public interface FailureDefinition {
  URI getURI();
}

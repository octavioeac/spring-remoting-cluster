package com.proemion.spring.clustering;

import java.net.URI;


/**
 * A Definition to be filled by a {@link ProtocolHandler}
 * A {@link ProtocolHandler} can implement this interface and store implementation specific protocol information for a single {@link RemoteService} in it.
 * @author Steve Ulrich
 *
 */
public interface ProtocolDefinition {
  
  URI getURI();
}

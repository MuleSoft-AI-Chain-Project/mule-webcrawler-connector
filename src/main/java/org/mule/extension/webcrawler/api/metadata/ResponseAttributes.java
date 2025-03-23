package org.mule.extension.webcrawler.api.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.mule.runtime.extension.api.annotation.param.MediaType;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ResponseAttributes implements Serializable {

  /**
   * Additional attributes not explicitly defined as fields in this class.
   */
  protected final HashMap<String, Object> otherAttributes;

  public ResponseAttributes(HashMap<String, Object> requestAttributes) {

    this.otherAttributes = requestAttributes;
  }

  /**
   * Gets additional attributes of the document.
   * <p>
   * These are attributes not explicitly defined in this class.
   *
   * @return a map of additional attributes.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  public Map<String, Object> getOtherAttributes() {
    return otherAttributes;
  }
}

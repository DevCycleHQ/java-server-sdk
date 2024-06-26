package com.devcycle.sdk.server.local.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RequestEvent {

    @Schema(required = true, description = "Custom event type")
    private String customType;

    @Schema(description = "DevCycleEvent type")
    @Builder.Default
    private String type = "customEvent";

    @Schema(description = "User ID")
    private String user_id;

    @Schema(description = "Custom event target / subject of event. Contextual to event type")
    private String target;

    @Schema(description = "Unix epoch time the event occurred according to client")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'hh:mm:ss.SSS'Z'")
    private String date;

    @Schema(description = "Unix epoch time the event occurred according to client")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'hh:mm:ss.SSS'Z'")
    private Date clientDate;

    @Schema(description = "Value for numerical events. Contextual to event type")
    private BigDecimal value;

    @Schema(description = "Extra JSON metadata for event. Contextual to event type")
    private Object metaData;

    @Schema(description = "Feature variation map")
    private Map<String, String> featureVars;
}

package cn.cordys.crm.health.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SyncRequest {

    @Schema(description = "档案ID")
    private String id;
}

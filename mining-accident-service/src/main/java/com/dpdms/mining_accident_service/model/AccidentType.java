package com.dpdms.mining_accident_service.model;

// What actually happened at the mine/site. Recorders pick one.
public enum AccidentType {
    ROCKFALL,
    FLOODING,
    GAS_LEAK,
    EQUIPMENT_FAILURE,
    OTHER
}

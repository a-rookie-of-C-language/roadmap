package com.roadmap.dto;

import java.util.ArrayList;
import java.util.List;

public class LocationBatchDTO {

    private List<LocationDTO> locations = new ArrayList<>();

    public List<LocationDTO> getLocations() {
        return locations;
    }

    public void setLocations(List<LocationDTO> locations) {
        this.locations = locations;
    }
}

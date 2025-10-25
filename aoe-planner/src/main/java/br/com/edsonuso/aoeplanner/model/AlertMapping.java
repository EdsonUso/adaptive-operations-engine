package br.com.edsonuso.aoeplanner.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class AlertMapping {
    private String alertName;
    private Map<String, String> matches;
    private List<FactMapping> facts;
}

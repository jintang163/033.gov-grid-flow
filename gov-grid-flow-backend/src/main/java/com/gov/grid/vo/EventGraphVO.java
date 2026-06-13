package com.gov.grid.vo;

import lombok.Data;

import java.util.List;

@Data
public class EventGraphVO {

    private Long eventId;

    private String eventNo;

    private String title;

    private List<GraphNode> nodes;

    private List<GraphEdge> edges;

    private RecurrenceGroupVO recurrenceGroup;

    @Data
    public static class GraphNode {
        private String id;
        private String label;
        private String type;
        private Integer symbolSize;
        private Integer category;
        private String description;
        private Double x;
        private Double y;
    }

    @Data
    public static class GraphEdge {
        private String source;
        private String target;
        private String label;
        private String relationType;
    }
}

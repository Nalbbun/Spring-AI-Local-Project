package ai.local.nalbbun.category.travel.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleItem {
    private String time;
    private String type;   // attraction, meal, accommodation
    private String name;
    private String address;
    private String description;
    private int cost;
}
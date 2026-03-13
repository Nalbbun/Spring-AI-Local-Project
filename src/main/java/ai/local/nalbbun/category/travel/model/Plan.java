package ai.local.nalbbun.category.travel.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Plan {
    private List<DaySchedule> days;
    private Integer maxBudget;
    private Integer totalCost;
    private Integer meals;
    private Integer accommodation;
    private Integer attractions;
}
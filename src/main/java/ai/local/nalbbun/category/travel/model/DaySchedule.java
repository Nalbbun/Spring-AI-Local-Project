package ai.local.nalbbun.category.travel.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DaySchedule {
    private int dayNumber;
    private List<ScheduleItem> schedule;
}
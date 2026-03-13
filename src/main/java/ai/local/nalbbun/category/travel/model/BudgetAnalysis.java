package ai.local.nalbbun.category.travel.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetAnalysis {
    private Integer maxBudget;
    private Integer actualTotalCost;
    private boolean exceeded;
    private String message;
}
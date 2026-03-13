package ai.local.nalbbun.category.travel.model;

import ai.local.nalbbun.model.common.CategoryContext;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TravelContext implements CategoryContext {

    private String userQuery;
    private String destination;
    private Integer days;
    private Integer maxBudget;

    private String parserMode;

    private List<Attraction> attractions = new ArrayList<>();
    private List<Restaurant> restaurants = new ArrayList<>();
    private List<Accommodation> accommodations = new ArrayList<>();

    private BudgetAnalysis budgetAnalysis;
    private Plan plan;

    private boolean replan;
    private Integer previousTotalCost;
}
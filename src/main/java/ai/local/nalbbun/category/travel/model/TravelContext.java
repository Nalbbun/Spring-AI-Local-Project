package ai.local.nalbbun.category.travel.model;

import ai.local.nalbbun.model.common.CategoryContext;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * TravelContext는 계층 간에 전달되는 입력 및 출력 데이터를 표현하는 모델이다.
 * <p>주요 기능: travel context 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Data
public class TravelContext implements CategoryContext {

    /** userQuery 값을 보관한다. */
    private String userQuery;
    /** destination 값을 보관한다. */
    private String destination;
    /** days 값을 보관한다. */
    private Integer days;
    /** maxBudget 값을 보관한다. */
    private Integer maxBudget;

    /** parserMode 값을 보관한다. */
    private String parserMode;

    /** attractions 값을 보관한다. */
    private List<Attraction> attractions = new ArrayList<>();
    /** restaurants 값을 보관한다. */
    private List<Restaurant> restaurants = new ArrayList<>();
    /** accommodations 값을 보관한다. */
    private List<Accommodation> accommodations = new ArrayList<>();

    /** budgetAnalysis 값을 보관한다. */
    private BudgetAnalysis budgetAnalysis;
    /** plan 값을 보관한다. */
    private Plan plan;

    /** replan 값을 보관한다. */
    private boolean replan;
    /** previousTotalCost 값을 보관한다. */
    private Integer previousTotalCost;
}
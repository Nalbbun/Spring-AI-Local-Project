package ai.local.nalbbun.category.travel.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Restaurant {
    private String name;
    private String address;
    private String description;
    private int price;
}
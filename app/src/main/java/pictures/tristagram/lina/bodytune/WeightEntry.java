package pictures.tristagram.lina.bodytune;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "weight_history")
public class WeightEntry {
    @PrimaryKey
    @NonNull
    public String date;

    public String goalType;
    public Float targetWeight;
    public Float weight;
    public Float waist;
    public Float chest;
    public Float hips;
    public Float bicep;

    public WeightEntry(@NonNull String date, float weight) {
        this.date = date;
        this.weight = weight;
    }
}
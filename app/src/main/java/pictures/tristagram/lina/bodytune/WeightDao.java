package pictures.tristagram.lina.bodytune;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface WeightDao {
    // Вставляем новую запись. Если дата уже есть — заменяем её
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WeightEntry entry);

    // Получаем все записи, отсортированные по дате для графика
    @Query("SELECT * FROM weight_history ORDER BY date ASC")
    List<WeightEntry> getAllEntries();

    // Удаление записи (если понадобится в истории)
    @Query("DELETE FROM weight_history WHERE date = :date")
    void deleteByDate(String date);

    @Query("SELECT * FROM weight_history WHERE date = :date LIMIT 1")
    WeightEntry getEntryByDate(String date);

    @Query("SELECT * FROM weight_history WHERE date < :date ORDER BY date DESC LIMIT 1")
    WeightEntry getLatestEntryBefore(String date);

    @Query("SELECT * FROM weight_history ORDER BY date DESC LIMIT 1")
    WeightEntry getLastAnyEntry();
}
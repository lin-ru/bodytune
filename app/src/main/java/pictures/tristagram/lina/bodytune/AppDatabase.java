package pictures.tristagram.lina.bodytune;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {WeightEntry.class}, version =1)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract WeightDao weightDao();

    // Синхронизированный метод для получения экземпляра БД (Singleton)
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "bodytune_db")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
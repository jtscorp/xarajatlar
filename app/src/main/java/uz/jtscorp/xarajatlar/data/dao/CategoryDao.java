package uz.jtscorp.xarajatlar.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;
import uz.jtscorp.xarajatlar.data.entity.Category;

@Dao
public interface CategoryDao {
    @Insert
    long insert(Category category);

    @Update
    void update(Category category);

    @Delete
    void delete(Category category);

    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY name")
    LiveData<List<Category>> getAllActive();

    @Query("SELECT * FROM categories ORDER BY name")
    LiveData<List<Category>> getAll();

    @Query("SELECT * FROM categories WHERE id = :id")
    Category getById(int id);

    @Query("SELECT COUNT(*) FROM categories")
    int getCount();

    @Query("SELECT * FROM categories")
    List<Category> getAllSync();

    @Query("DELETE FROM categories")
    void deleteAll();
}

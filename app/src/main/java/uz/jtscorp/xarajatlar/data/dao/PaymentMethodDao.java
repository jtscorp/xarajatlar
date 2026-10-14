package uz.jtscorp.xarajatlar.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;
import uz.jtscorp.xarajatlar.data.entity.PaymentMethod;

@Dao
public interface PaymentMethodDao {
    @Insert
    long insert(PaymentMethod method);

    @Update
    void update(PaymentMethod method);

    @Delete
    void delete(PaymentMethod method);

    @Query("SELECT * FROM payment_methods ORDER BY name")
    LiveData<List<PaymentMethod>> getAll();

    @Query("SELECT * FROM payment_methods WHERE id = :id")
    PaymentMethod getById(int id);

    @Query("SELECT * FROM payment_methods")
    List<PaymentMethod> getAllSync();

    @Query("DELETE FROM payment_methods")
    void deleteAll();
}

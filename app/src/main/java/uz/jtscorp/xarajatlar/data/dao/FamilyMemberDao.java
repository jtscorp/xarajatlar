package uz.jtscorp.xarajatlar.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;
import uz.jtscorp.xarajatlar.data.entity.FamilyMember;

@Dao
public interface FamilyMemberDao {
    @Insert
    long insert(FamilyMember member);

    @Update
    void update(FamilyMember member);

    @Delete
    void delete(FamilyMember member);

    @Query("SELECT * FROM family_members WHERE isActive = 1 ORDER BY name")
    LiveData<List<FamilyMember>> getAllActive();

    @Query("SELECT * FROM family_members ORDER BY name")
    LiveData<List<FamilyMember>> getAll();

    @Query("SELECT * FROM family_members WHERE id = :id")
    FamilyMember getById(int id);

    @Query("SELECT * FROM family_members")
    List<FamilyMember> getAllSync();

    @Query("DELETE FROM family_members")
    void deleteAll();
}

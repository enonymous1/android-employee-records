package com.enonymous1.employeerecords.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import com.enonymous1.employeerecords.model.Employee
import com.enonymous1.employeerecords.model.SortBy
import com.enonymous1.employeerecords.model.SortOrder
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** One row per employee. The id is the identity used for details, edit, and delete. */
@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "first_name") val firstName: String,
    @ColumnInfo(name = "last_name") val lastName: String,
    @ColumnInfo(name = "employee_number") val employeeNumber: Int,
    @ColumnInfo(name = "hire_date") val hireDate: LocalDate,
    @ColumnInfo(name = "employment_status") val employmentStatus: String,
) {
    fun toDomain(): Employee = Employee(
        id = id,
        firstName = firstName,
        lastName = lastName,
        employeeNumber = employeeNumber,
        hireDate = hireDate,
        employmentStatus = employmentStatus,
    )

    companion object {
        fun fromDomain(employee: Employee): EmployeeEntity = EmployeeEntity(
            id = employee.id,
            firstName = employee.firstName,
            lastName = employee.lastName,
            employeeNumber = employee.employeeNumber,
            hireDate = employee.hireDate,
            employmentStatus = employee.employmentStatus,
        )
    }
}

/** Hire dates are stored as epoch days, not as free-form date strings. */
class Converters {
    @TypeConverter
    fun fromEpochDay(value: Long): LocalDate = LocalDate.ofEpochDay(value)

    @TypeConverter
    fun toEpochDay(date: LocalDate): Long = date.toEpochDay()
}

@Dao
interface EmployeesDao {
    @Query("SELECT * FROM employees ORDER BY employment_status COLLATE NOCASE ASC, id ASC")
    fun observeByStatusAsc(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees ORDER BY employment_status COLLATE NOCASE DESC, id ASC")
    fun observeByStatusDesc(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees ORDER BY employee_number ASC, id ASC")
    fun observeByNumberAsc(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees ORDER BY employee_number DESC, id ASC")
    fun observeByNumberDesc(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE id = :id")
    fun observeById(id: Long): Flow<EmployeeEntity?>

    @Insert
    suspend fun insert(employee: EmployeeEntity)

    @Update
    suspend fun update(employee: EmployeeEntity)

    @Delete
    suspend fun delete(employee: EmployeeEntity)

    @Query("DELETE FROM employees")
    suspend fun deleteAll()
}

@Database(entities = [EmployeeEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class EmployeesDatabase : RoomDatabase() {
    abstract fun employeesDao(): EmployeesDao
}

/** The data layer behind [com.enonymous1.employeerecords.EmployeesViewModel]. Kept as an interface so tests can fake it. */
interface EmployeesRepository {
    fun observeEmployees(sortBy: SortBy, sortOrder: SortOrder): Flow<List<Employee>>
    fun observeEmployee(id: Long): Flow<Employee?>
    suspend fun add(employee: Employee)
    suspend fun update(employee: Employee)
    suspend fun delete(employee: Employee)
    suspend fun deleteAll()
}

class RoomEmployeesRepository(private val dao: EmployeesDao) : EmployeesRepository {
    override fun observeEmployees(sortBy: SortBy, sortOrder: SortOrder): Flow<List<Employee>> {
        val entities = when (sortBy) {
            SortBy.STATUS -> when (sortOrder) {
                SortOrder.ASCENDING -> dao.observeByStatusAsc()
                SortOrder.DESCENDING -> dao.observeByStatusDesc()
            }
            SortBy.NUMBER -> when (sortOrder) {
                SortOrder.ASCENDING -> dao.observeByNumberAsc()
                SortOrder.DESCENDING -> dao.observeByNumberDesc()
            }
        }
        return entities.map { list -> list.map { it.toDomain() } }
    }

    override fun observeEmployee(id: Long): Flow<Employee?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun add(employee: Employee) {
        dao.insert(EmployeeEntity.fromDomain(employee))
    }

    override suspend fun update(employee: Employee) {
        dao.update(EmployeeEntity.fromDomain(employee))
    }

    override suspend fun delete(employee: Employee) {
        dao.delete(EmployeeEntity.fromDomain(employee))
    }

    override suspend fun deleteAll() {
        dao.deleteAll()
    }
}

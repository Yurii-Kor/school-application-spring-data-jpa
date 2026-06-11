package ua.foxminded.schoolapplication.model.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ua.foxminded.schoolapplication.model.domain.Group;

public interface GroupRepository extends JpaRepository<Group, Long> {

	@Query("""
			    SELECT g2.id
			    FROM Group g2
			    LEFT JOIN g2.students s2
			    GROUP BY g2.id
			    HAVING COUNT(s2.id) <= :maxStudents
			""")
	List<Long> findGroupIdsByStudentCountLessThanOrEqualTo(@Param("maxStudents") long maxStudents);
}

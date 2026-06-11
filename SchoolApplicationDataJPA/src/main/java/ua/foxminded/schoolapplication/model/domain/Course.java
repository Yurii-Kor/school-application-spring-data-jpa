package ua.foxminded.schoolapplication.model.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Set;
import java.util.HashSet;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "courses")
public class Course {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "course_id_seq")
	@SequenceGenerator(name = "course_id_seq", sequenceName = "course_id_seq", allocationSize = 1)
	@Column(name = "course_id", nullable = false)
	private Long id;

	@NotBlank
	@Size(min = 2, max = 100)
	@Pattern(regexp = "^[A-Za-z0-9\\s\\-:,.'&]+$")
	@Column(nullable = false, unique = true)
	private String courseName;

	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	@Size(max = 1000)
	@Basic(fetch = FetchType.LAZY)
	@Column(columnDefinition = "TEXT")
	private String courseDescription;

	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	@Builder.Default
	@ManyToMany(mappedBy = "courses")
	private Set<Student> students = new HashSet<>();
}
